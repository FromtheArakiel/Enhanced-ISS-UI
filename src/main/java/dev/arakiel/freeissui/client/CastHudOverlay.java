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

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The overlay itself: it owns the deck state and the reusable frame buffers, asks the state and the
 * view what this frame looks like, and hands the result to the painter.
 *
 * <p>Each frame takes the clock once, samples the deck once, and then draws straight from the
 * buffers that {@link DeckView} reuses. Nothing is allocated for a deck that is not changing, and
 * cards are culled before they reach the painter, so no scissor is needed either.
 */
public final class CastHudOverlay implements IGuiOverlay {

    public static final CastHudOverlay INSTANCE = new CastHudOverlay();

    private final DeckState state = new DeckState();
    private final DeckView view = new DeckView();
    private final CardPainter painter = new CardPainter();
    private final SpellInfoPanel infoPanel = new SpellInfoPanel();

    private CastHudOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft client = Minecraft.getInstance();

        // The debug screen (F3) owns the screen while it is open.
        if (client.options.renderDebug || client.options.hideGui) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || player.isSpectator()) {
            state.forget();
            return;
        }
        SpellSelectionManager selection = ClientMagicData.getSpellSelectionManager();
        if (selection == null) {
            state.forget();
            return;
        }
        List<SpellSelectionManager.SelectionOption> spells = selection.getAllSpells();
        int count = spells.size();
        if (count == 0) {
            state.forget();
            return;
        }

        long now = Util.getMillis();
        int focused = Mth.clamp(selection.getGlobalSelectionIndex(), 0, count - 1);
        DeckState.Change change = state.visit(focused, count, now);
        if (change != null) {
            DeckSounds.step(change.direction());
        }
        state.sample(now, ClientMagicData.isCasting());

        float centerY = height * 0.5F - DeckTuning.CARD_HALF;
        DeckView.Frame frame = view.compose(state, count, centerY, height);
        painter.paint(graphics, frame, spells, state);
        state.settle(now);

        if (Screen.hasAltDown()) {
            infoPanel.paint(graphics, DeckTuning.DECK_X, centerY, spells.get(focused), player, focused, count);
        }
    }
}
