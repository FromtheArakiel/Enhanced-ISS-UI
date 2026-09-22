/*
 * Free ISS UI - a client side HUD rework for Iron's Spells 'n Spellbooks.
 * Copyright (C) 2026 FromtheArakiel
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package dev.arakiel.freeissui.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.util.Mth;

/**
 * Turns the deck state into concrete card placements for one frame.
 *
 * <p>Working out <em>where</em> everything is happens here, drawing it happens in
 * {@link CardPainter}. Both the frame and its cards are kept between frames and the ring of visible
 * card offsets is only recalculated when the spell count changes, so a steady frame allocates
 * nothing at all.
 */
final class DeckView {

    /** One card of the deck. Instances are reused frame after frame. */
    static final class Card {
        int index;
        float x;
        float y;
        float zoom;
        float alpha;
        boolean focused;
    }

    /** Placements of a single frame; only the first {@link #count} entries are in use. */
    static final class Frame {
        final List<Card> cards = new ArrayList<>();
        int count;
        float haloAlpha;
        boolean arrow;
        float arrowStrength;
        int arrowDirection;
    }

    /** Rings further out than this cannot reach the screen anyway. */
    private static final int MAX_RINGS = 64;

    private final Frame frame = new Frame();
    private final int[] rings = new int[MAX_RINGS * 2];
    private boolean[] taken = new boolean[8];
    private int ringCount;
    private int ringCacheFor = -1;

    Frame compose(DeckState state, int count, float centerY, int screenHeight) {
        frame.count = 0;
        if (state.moving() && state.motion() == DeckState.Motion.SWOOP) {
            swoop(state, count, centerY);
        } else {
            stack(state, count, centerY, screenHeight);
        }
        return frame;
    }

    /** The everyday deck: a centred column of cards with the focused one drawn last. */
    private void stack(DeckState state, int count, float centerY, int screenHeight) {
        if (count != ringCacheFor) {
            cacheRings(count);
        }
        float pitch = Mth.lerp(state.unfold(), DeckTuning.PITCH_TIGHT, DeckTuning.PITCH_LOOSE);
        float slide = state.slideOffset();

        for (int i = 0; i < ringCount; i++) {
            place(state, count, rings[i], pitch, slide, centerY, screenHeight);
        }

        float halo = 0.0F;
        Card focused = place(state, count, 0, pitch, slide, centerY, screenHeight);
        if (focused != null) {
            float closeness = 1.0F - Mth.clamp(
                    Math.abs(focused.y - centerY) / DeckTuning.PITCH_LOOSE, 0.0F, 1.0F);
            halo = state.unfold() * closeness;
        }

        frame.haloAlpha = halo;
        frame.arrow = state.moving();
        frame.arrowStrength = state.moving() ? state.progress() : 0.0F;
        frame.arrowDirection = state.direction();
    }

    /** The spell wheel hand-over: the old card steps aside while the new one is pulled in. */
    private void swoop(DeckState state, int count, float centerY) {
        int focus = state.focus();
        int leaving = state.leaving();
        float raw = state.progress();
        float eased = DeckState.ease(raw);

        for (int index = 0; index < count; index++) {
            if (index == focus || index == leaving) {
                continue;
            }
            int before = DeckState.wrapDelta(index - leaving, count);
            int after = DeckState.wrapDelta(index - focus, count);
            float alpha = Mth.lerp(eased, coreAlpha(before), coreAlpha(after));
            if (alpha <= 0.001F) {
                continue;
            }
            Card card = borrow();
            card.index = index;
            card.x = DeckTuning.DECK_X;
            card.y = Mth.lerp(eased, rowY(before, centerY), rowY(after, centerY));
            card.zoom = 1.0F;
            card.alpha = alpha;
            card.focused = false;
        }

        if (leaving >= 0 && leaving != focus) {
            int relative = DeckState.wrapDelta(leaving - focus, count);
            float retreat = Mth.sin(raw * (float) Math.PI);
            Card card = borrow();
            card.index = leaving;
            card.x = DeckTuning.DECK_X - retreat * DeckTuning.SWOOP_LEAVE_DX;
            card.y = Mth.lerp(raw, centerY, rowY(relative, centerY));
            card.zoom = 1.0F;
            card.alpha = Mth.lerp(raw, 1.0F,
                    Math.abs(relative) <= DeckTuning.STACK_RADIUS ? 1.0F : 0.0F);
            card.focused = false;
        }

        int incoming = DeckState.wrapDelta(focus - leaving, count);
        float pull = DeckState.ease(raw / DeckTuning.SWOOP_PULL_END);
        float insert = DeckState.ease((raw - DeckTuning.SWOOP_PULL_END) / (1.0F - DeckTuning.SWOOP_PULL_END));
        float pullX = Mth.lerp(pull, DeckTuning.DECK_X + DeckTuning.SWOOP_PULL_DX,
                DeckTuning.DECK_X + DeckTuning.SWOOP_OVERSHOOT_DX);
        float pullY = Mth.lerp(pull, rowY(incoming, centerY),
                centerY + state.direction() * DeckTuning.SWOOP_TRAVEL_DY);

        Card focused = borrow();
        focused.index = focus;
        focused.x = Mth.lerp(insert, pullX, DeckTuning.DECK_X);
        focused.y = Mth.lerp(insert, pullY, centerY);
        focused.zoom = Mth.lerp(insert, DeckTuning.SWOOP_START_ZOOM, 1.0F);
        focused.alpha = 1.0F;
        focused.focused = true;

        frame.haloAlpha = 0.0F;
        frame.arrow = true;
        frame.arrowStrength = raw;
        frame.arrowDirection = state.direction();
    }

    private Card place(DeckState state, int count, int offset, float pitch, float slide,
                       float centerY, int screenHeight) {
        int index = Math.floorMod(state.focus() + offset, count);
        float y = centerY + (offset + slide) * pitch;
        float unfold = state.unfold();
        float alpha = Math.abs(offset) <= DeckTuning.STACK_RADIUS ? 1.0F : unfold;
        if (unfold > 0.0F) {
            alpha *= edgeFade(y, screenHeight);
        }
        if (alpha <= 0.001F || y + DeckTuning.CARD < 0.0F || y > screenHeight) {
            return null;
        }

        boolean focused = index == state.focus();
        Card card = borrow();
        card.index = index;
        card.x = DeckTuning.DECK_X;
        card.y = y;
        card.zoom = focused
                ? 1.0F + DeckTuning.CENTER_ZOOM * unfold
                        * (1.0F - Mth.clamp(Math.abs(y - centerY) / DeckTuning.PITCH_LOOSE, 0.0F, 1.0F))
                : 1.0F;
        card.alpha = alpha;
        card.focused = focused;
        return card;
    }

    private Card borrow() {
        if (frame.count == frame.cards.size()) {
            frame.cards.add(new Card());
        }
        return frame.cards.get(frame.count++);
    }

    /**
     * The offsets to draw only depend on how many spells there are, so the ring is worked out once
     * per spell count: far cards first, the focused one added by the caller.
     */
    private void cacheRings(int count) {
        ringCacheFor = count;
        ringCount = 0;
        if (taken.length < count) {
            taken = new boolean[count];
        } else {
            Arrays.fill(taken, 0, count, false);
        }
        int radius = Math.min(Math.max(1, count / 2), MAX_RINGS);
        for (int ring = radius; ring >= 1; ring--) {
            ringCount = addRing(count, ring, ringCount);
            ringCount = addRing(count, -ring, ringCount);
        }
    }

    private int addRing(int count, int offset, int size) {
        int slot = Math.floorMod(offset, count);
        if (taken[slot]) {
            return size;
        }
        taken[slot] = true;
        rings[size] = offset;
        return size + 1;
    }

    private static float rowY(int relative, float centerY) {
        return centerY + relative * DeckTuning.PITCH_LOOSE;
    }

    /** Cards next to the selection are always drawn; the rest fade in with the unfolded deck. */
    private static float coreAlpha(int relative) {
        return Math.abs(relative) <= DeckTuning.STACK_RADIUS ? 1.0F : 0.0F;
    }

    private static float edgeFade(float y, int screenHeight) {
        float middle = y + DeckTuning.CARD_HALF;
        float halfSpan = Math.max(1.0F, screenHeight * 0.5F - DeckTuning.CARD_HALF);
        return 1.0F - DeckState.ease(Math.abs(middle - screenHeight * 0.5F) / halfSpan);
    }
}
