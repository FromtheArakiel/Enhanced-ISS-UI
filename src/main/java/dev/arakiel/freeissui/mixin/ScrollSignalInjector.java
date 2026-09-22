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

package dev.arakiel.freeissui.mixin;

import dev.arakiel.freeissui.signal.HudSignals;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Notes that the player stepped through spells with the scroll wheel.
 * Only the fact is reported - the deck works out the rest on its own.
 */
@Mixin(value = ClientInputEvents.class, remap = false)
public abstract class ScrollSignalInjector {

    @Inject(method = "handleSpellBarScrollModifier(I)Z", at = @At("HEAD"), require = 1, remap = false)
    private static void freeissui$noteScrollStep(int step, CallbackInfoReturnable<Boolean> callback) {
        SpellSelectionManager selection = ClientMagicData.getSpellSelectionManager();
        if (selection != null) {
            HudSignals.note(HudSignals.Kind.SCROLL_STEP, selection.getGlobalSelectionIndex(), Util.getMillis());
        }
    }
}
