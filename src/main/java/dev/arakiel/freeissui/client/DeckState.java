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

import dev.arakiel.freeissui.signal.HudSignals;
import net.minecraft.util.Mth;

/**
 * Everything the deck remembers between two frames: the focused spell, how far the deck has
 * unfolded, which transition is in flight and how far the casting pulse has come along.
 *
 * <p>All timings are sampled once per frame in {@link #sample(long, boolean)}. The view and the
 * painter then read the cached numbers, so a frame that draws a dozen cards still walks the clock
 * and the easing curves a single time.
 */
final class DeckState {

    enum Motion {
        /** Nothing is animating. */
        STILL,
        /** A single step, normally from the scroll wheel: the deck slides back into place. */
        SLIDE,
        /** A pick from the spell wheel: the old card leaves and the new one swoops in. */
        SWOOP
    }

    /** Report of a spell change, handed to the HUD so it can play the click. */
    record Change(HudSignals.Kind kind, int from, int to, int direction) {
    }

    private int cards;
    private int focus = -1;
    private int leaving = -1;
    private int direction = 1;

    private long unfoldStamp = Long.MIN_VALUE;

    private Motion motion = Motion.STILL;
    private long motionStamp = Long.MIN_VALUE;
    private float slideCards;

    private long pulseStamp = Long.MIN_VALUE;
    private long pulseHold = Long.MIN_VALUE;

    /* numbers sampled for the frame that is being drawn */
    private float frameUnfold;
    private float frameSlide;
    private float frameProgress;
    private boolean frameMoving;
    private boolean framePulseActive;
    private float framePulseZoom = 1.0F;
    private float framePulseSpin;

    int focus() {
        return focus;
    }

    int leaving() {
        return leaving;
    }

    int direction() {
        return direction;
    }

    Motion motion() {
        return motion;
    }

    int cardCount() {
        return cards;
    }

    boolean moving() {
        return frameMoving;
    }

    float progress() {
        return frameProgress;
    }

    float slideOffset() {
        return frameSlide;
    }

    float unfold() {
        return frameUnfold;
    }

    boolean pulseActive() {
        return framePulseActive;
    }

    float pulseZoom() {
        return framePulseZoom;
    }

    float pulseSpin() {
        return framePulseSpin;
    }

    /** Samples every timing of the current frame in one pass. */
    void sample(long now, boolean casting) {
        frameProgress = motion == Motion.STILL ? 1.0F : progressAt(now);
        frameMoving = motion != Motion.STILL && frameProgress < 1.0F;
        frameSlide = motion == Motion.SLIDE ? slideCards * (1.0F - ease(frameProgress)) : 0.0F;
        frameUnfold = unfoldAt(now);
        samplePulse(now, casting);
    }

    /**
     * Feeds the deck the current spell selection. Returns a change report when the player actually
     * switched spells this frame, or {@code null} when nothing happened.
     */
    Change visit(int index, int count, long now) {
        cards = count;
        if (focus < 0) {
            focus = index;
            return null;
        }
        if (index == focus) {
            return null;
        }

        HudSignals.Note pick = HudSignals.take(HudSignals.Kind.WHEEL_PICK, now, DeckTuning.SIGNAL_TTL_MS);
        HudSignals.Note step = pick != null
                ? null
                : HudSignals.take(HudSignals.Kind.SCROLL_STEP, now, DeckTuning.SIGNAL_TTL_MS);

        int delta = wrapDelta(index - focus, count);
        float carried = slideAt(now);
        int from = focus;

        leaving = from;
        direction = delta < 0 ? -1 : 1;
        motion = pick != null ? Motion.SWOOP : Motion.SLIDE;
        motionStamp = now;
        if (motion == Motion.SWOOP) {
            slideCards = 0.0F;
            unfoldStamp = Long.MIN_VALUE;
        } else {
            slideCards = carried + delta;
            unfoldStamp = now;
        }
        focus = index;

        HudSignals.Kind kind = pick != null
                ? pick.kind()
                : step != null ? step.kind() : HudSignals.Kind.SCROLL_STEP;
        return new Change(kind, from, index, direction);
    }

    /** Parks the animation once it has run to its end. */
    void settle(long now) {
        if (motion != Motion.STILL && progressAt(now) >= 1.0F) {
            motion = Motion.STILL;
            leaving = -1;
            slideCards = 0.0F;
        }
    }

    /** Drops every bit of memory, used whenever the deck is not on screen at all. */
    void forget() {
        cards = 0;
        focus = -1;
        leaving = -1;
        direction = 1;
        unfoldStamp = Long.MIN_VALUE;
        motion = Motion.STILL;
        motionStamp = Long.MIN_VALUE;
        slideCards = 0.0F;
        pulseStamp = Long.MIN_VALUE;
        pulseHold = Long.MIN_VALUE;
        frameUnfold = 0.0F;
        frameSlide = 0.0F;
        frameProgress = 0.0F;
        frameMoving = false;
        framePulseActive = false;
        framePulseZoom = 1.0F;
        framePulseSpin = 0.0F;
        HudSignals.clear();
    }

    private float progressAt(long now) {
        long span = motion == Motion.SWOOP ? DeckTuning.SWOOP_MS : DeckTuning.SLIDE_MS;
        return Mth.clamp((now - motionStamp) / (float) span, 0.0F, 1.0F);
    }

    private float slideAt(long now) {
        return motion == Motion.SLIDE ? slideCards * (1.0F - ease(progressAt(now))) : 0.0F;
    }

    private float unfoldAt(long now) {
        if (unfoldStamp == Long.MIN_VALUE) {
            return 0.0F;
        }
        long quiet = now - unfoldStamp;
        if (quiet <= DeckTuning.UNFOLD_KEEP_MS) {
            return 1.0F;
        }
        return 1.0F - ease((quiet - DeckTuning.UNFOLD_KEEP_MS) / (float) DeckTuning.UNFOLD_FADE_MS);
    }

    /**
     * Casting is polled rather than reported by a patch, and the pulse is held for a moment after
     * casting stops so that very short casts still read on screen.
     */
    private void samplePulse(long now, boolean casting) {
        if (casting) {
            if (pulseStamp == Long.MIN_VALUE) {
                pulseStamp = now;
            }
            pulseHold = Math.max(pulseHold, now + DeckTuning.PULSE_MIN_MS);
        }
        if (pulseStamp == Long.MIN_VALUE || (!casting && now >= pulseHold)) {
            pulseStamp = Long.MIN_VALUE;
            pulseHold = Long.MIN_VALUE;
            framePulseActive = false;
            framePulseZoom = 1.0F;
            framePulseSpin = 0.0F;
            return;
        }
        long lived = Math.max(0L, now - pulseStamp);
        float entry = ease(lived / DeckTuning.PULSE_ENTRY_MS);
        float breath = 0.5F + 0.5F * Mth.sin(lived * DeckTuning.PULSE_SPEED);
        framePulseActive = true;
        framePulseZoom = 1.0F + entry * (DeckTuning.PULSE_GAIN + breath * DeckTuning.PULSE_BREATH);
        framePulseSpin = (lived % DeckTuning.PULSE_SPIN_MS) * (360.0F / DeckTuning.PULSE_SPIN_MS);
    }

    /** Shortest signed distance from {@code center} to {@code index} on a ring of {@code count}. */
    static int wrapDelta(int raw, int count) {
        if (count <= 0) {
            return 0;
        }
        int half = count / 2;
        int delta = raw;
        if (delta > half) {
            delta -= count;
        } else if (delta < -half) {
            delta += count;
        }
        return delta;
    }

    /** Smooth (quintic) 0..1 ramp. */
    static float ease(float raw) {
        float t = Mth.clamp(raw, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }
}
