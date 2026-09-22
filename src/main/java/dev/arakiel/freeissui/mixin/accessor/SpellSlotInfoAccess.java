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

package dev.arakiel.freeissui.mixin.accessor;

import net.minecraft.client.gui.components.Button;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The inscription table keeps its spell slots in a package private inner class, so this interface
 * is the only way to move a slot around and to reach the button behind it.
 */
@Mixin(targets = "io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen$SpellSlotInfo", remap = false)
public interface SpellSlotInfoAccess {

    @Accessor(value = "relativePosition", remap = false)
    void freeissui$moveTo(Vec2 position);

    @Accessor(value = "button", remap = false)
    Button freeissui$button();
}
