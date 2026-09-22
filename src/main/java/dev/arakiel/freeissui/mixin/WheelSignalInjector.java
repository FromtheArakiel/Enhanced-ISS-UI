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
import io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Notes that the spell wheel is being closed, which is when it applies the picked spell.
 * The index itself is read by the deck, so nothing has to be captured here.
 */
@Mixin(value = SpellWheelOverlay.class, remap = false)
public abstract class WheelSignalInjector {

    @Inject(method = "close()V", at = @At("HEAD"), require = 1, remap = false)
    private void freeissui$noteWheelPick(CallbackInfo callback) {
        SpellSelectionManager selection = ClientMagicData.getSpellSelectionManager();
        if (selection != null) {
            HudSignals.note(HudSignals.Kind.WHEEL_PICK, selection.getGlobalSelectionIndex(), Util.getMillis());
        }
    }
}
