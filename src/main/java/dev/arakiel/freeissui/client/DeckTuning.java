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

/**
 * Every number that shapes the spell deck: sizes, timings, atlas coordinates and colours.
 * Keeping them together means the drawing and animation code stays free of bare literals.
 */
public final class DeckTuning {

    private DeckTuning() {
    }

    /* ---------- geometry ---------- */

    public static final int CARD = 22;
    public static final int CARD_HALF = CARD / 2;
    public static final int CARD_ICON = 16;

    /** Left edge of the deck. */
    public static final int DECK_X = 13;

    /** Vertical distance between two cards, tight while idle and loose while scrolling. */
    public static final float PITCH_TIGHT = 6.0F;
    public static final float PITCH_LOOSE = 24.0F;

    /* ---------- deck behaviour ---------- */

    /** How many cards remain visible on each side while the deck is folded. */
    public static final int STACK_RADIUS = 2;

    /** Extra size of the focused card once the deck is fully unfolded. */
    public static final float CENTER_ZOOM = 0.1F;

    public static final long UNFOLD_KEEP_MS = 620L;
    public static final long UNFOLD_FADE_MS = 180L;

    public static final long SLIDE_MS = 180L;
    public static final long SWOOP_MS = 300L;

    /** How long an input note stays relevant for the HUD. */
    public static final long SIGNAL_TTL_MS = 1000L;

    /** Split of the swoop animation: pulling the incoming card out, then slotting it in. */
    public static final float SWOOP_PULL_END = 0.38F;
    public static final float SWOOP_PULL_DX = -6.0F;
    public static final float SWOOP_OVERSHOOT_DX = 16.0F;
    public static final float SWOOP_TRAVEL_DY = 4.0F;
    public static final float SWOOP_START_ZOOM = 0.96F;
    public static final float SWOOP_LEAVE_DX = 8.0F;

    /* ---------- casting pulse ---------- */

    public static final long PULSE_MIN_MS = 420L;
    public static final float PULSE_ENTRY_MS = 150.0F;
    public static final float PULSE_GAIN = 0.085F;
    public static final float PULSE_BREATH = 0.032F;
    public static final float PULSE_SPEED = 0.012F;
    public static final long PULSE_SPIN_MS = 1400L;

    /* ---------- atlas coordinates inside SpellBarOverlay.TEXTURE ---------- */

    public static final int CARD_U = 66;
    public static final int FRAME_V = 84;
    public static final int FRAME_FOCUSED_U = 0;
    public static final int FRAME_SPELLBOOK_U = 22;
    public static final int FRAME_OTHER_U = 132;

    /* ---------- colours, alpha is applied per draw call ---------- */

    public static final int HALO_OUTER = 0x55336C;
    public static final int HALO_INNER = 0x09060F;
    public static final int COOLDOWN = 0x070512;
    public static final int ARROW = 0xE6B7FF;
    public static final int TEXT_TITLE = 0xFFF4F4F4;
    public static final int TEXT_BODY = 0xFFC8C8C8;

    public static final int HALO_OUTER_ALPHA = 138;
    public static final int HALO_INNER_ALPHA = 220;
    public static final int COOLDOWN_ALPHA = 217;

    /* ---------- info sheet ---------- */

    public static final float SHEET_SCALE = 0.72F;
    public static final int SHEET_LINE = 10;
    public static final int SHEET_X = CARD + 13;
    public static final int SHEET_Y = -4;

    /* ---------- selection sound ---------- */

    public static final float CLICK_PITCH = 1.32F;
    public static final float CLICK_PITCH_UP = 0.04F;
    public static final float CLICK_PITCH_DOWN = -0.02F;
    public static final float CLICK_PITCH_JITTER = 0.1F;
    public static final float CLICK_VOLUME = 0.42F;
    public static final float TICK_PITCH_RATIO = 1.08F;
    public static final float TICK_VOLUME = 0.14F;

    /* ---------- inscription table ---------- */

    public static final int TABLE_COLUMNS = 5;
    public static final int TABLE_PITCH = 19;
    public static final int TABLE_ROWS = 3;

    public static final int TABLE_BG_X = 67;
    public static final int TABLE_BG_Y = 15;
    public static final int TABLE_BG_W = 95;
    public static final int TABLE_BG_H = 57;

    public static final int TABLE_BAR_X = 165;
    public static final int TABLE_BAR_W = 3;

    /** Width the inscription table background was drawn for. */
    public static final int TABLE_PANEL_W = 256;

    public static final int TABLE_BAR_TRACK = 0x66333333;
    public static final int TABLE_BAR_THUMB = 0xFFCCCCCC;
}
