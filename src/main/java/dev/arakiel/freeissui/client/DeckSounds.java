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

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** The small dial click that follows a spell change. */
final class DeckSounds {

    private DeckSounds() {
    }

    static void step(int direction) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        float jitter = (player.getRandom().nextFloat() - 0.5F) * DeckTuning.CLICK_PITCH_JITTER;
        float pitch = DeckTuning.CLICK_PITCH
                + (direction < 0 ? DeckTuning.CLICK_PITCH_UP : DeckTuning.CLICK_PITCH_DOWN)
                + jitter;
        client.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK.value(), pitch, DeckTuning.CLICK_VOLUME));
        client.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.NOTE_BLOCK_HAT.value(), pitch * DeckTuning.TICK_PITCH_RATIO, DeckTuning.TICK_VOLUME));
    }
}
