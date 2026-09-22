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

package dev.arakiel.freeissui.signal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Mailbox between the patches and the HUD.
 *
 * <p>The patches only report <em>what the player did</em> - a scroll step, a pick from the spell
 * wheel - together with the spell index that was selected when it happened. What that means for
 * the animation, the sound or the layout is decided by the HUD alone, which keeps presentation
 * logic out of the patches entirely.
 */
public final class HudSignals {

    public enum Kind {
        SCROLL_STEP,
        WHEEL_PICK
    }

    public record Note(Kind kind, int indexBefore, long millis) {
    }

    private static final Deque<Note> NOTES = new ArrayDeque<>(8);
    private static final int CAPACITY = 8;

    private HudSignals() {
    }

    /** Called from the patches; never blocks and never throws. */
    public static synchronized void note(Kind kind, int indexBefore, long millis) {
        while (NOTES.size() >= CAPACITY) {
            NOTES.pollFirst();
        }
        NOTES.addLast(new Note(kind, indexBefore, millis));
    }

    /**
     * Removes and returns the newest note of the given kind, dropping every stale note on the way.
     * Returns {@code null} when nothing of that kind is pending.
     */
    public static synchronized Note take(Kind kind, long now, long ttl) {
        dropStale(now, ttl);
        Note taken = null;
        for (Iterator<Note> iterator = NOTES.iterator(); iterator.hasNext(); ) {
            Note note = iterator.next();
            if (note.kind() == kind) {
                taken = note;
                iterator.remove();
            }
        }
        return taken;
    }

    public static synchronized void clear() {
        NOTES.clear();
    }

    private static void dropStale(long now, long ttl) {
        NOTES.removeIf(note -> now - note.millis() > ttl);
    }
}
