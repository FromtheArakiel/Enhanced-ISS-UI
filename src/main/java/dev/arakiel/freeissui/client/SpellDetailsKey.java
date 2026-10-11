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

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The key that prints the spell sheet beside the deck.
 *
 * <p>It used to be a hard coded Alt check, which meant the sheet could not be moved off Alt. It is a
 * normal key mapping now: left Alt out of the box, rebindable in {@code Options -> Controls}. The
 * mapping lives in the in game conflict context, so the sheet only reacts while the player is
 * actually playing and the key never fights with a menu binding.
 */
public final class SpellDetailsKey {

    /** Category shown above the binding in the controls screen. */
    public static final String CATEGORY = "key.categories.freeissui";

    public static final KeyMapping SHOW_DETAILS = new KeyMapping("key.freeissui.spell_details",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    private SpellDetailsKey() {
    }

    /** Whether the configured details key is held down this frame. */
    public static boolean detailsHeld() {
        return SHOW_DETAILS.isDown();
    }
}
