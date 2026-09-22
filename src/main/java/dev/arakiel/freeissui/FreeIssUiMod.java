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

package dev.arakiel.freeissui;

import dev.arakiel.freeissui.client.CastHudOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Entry point of the mod.
 *
 * <p>Free ISS UI is a pure presentation layer for Iron's Spells 'n Spellbooks, so the only thing
 * that happens at load time is attaching the deck overlay to the hotbar on the client.
 */
@Mod(FreeIssUiMod.MODID)
public final class FreeIssUiMod {

    public static final String MODID = "freeissui";

    /** The mod whose HUD and inscription table this project dresses up. */
    public static final String PATCHED_MODID = "irons_spellbooks";

    public FreeIssUiMod() {
        // Nothing to initialise eagerly: the overlay is registered by the subscriber below and
        // every other behaviour is driven by the patches themselves.
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientWiring {

        @SubscribeEvent
        public static void attachOverlay(RegisterGuiOverlaysEvent event) {
            if (!ModList.get().isLoaded(PATCHED_MODID)) {
                return;
            }
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), MODID + "_deck", CastHudOverlay.INSTANCE);
        }
    }
}
