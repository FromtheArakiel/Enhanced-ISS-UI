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

import dev.arakiel.freeissui.client.DeckTuning;
import dev.arakiel.freeissui.mixin.accessor.SpellSlotInfoAccess;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns the inscription table's spell grid into a paged view.
 *
 * <p>Rows are spread evenly over the whole spell book (a book of seven spells becomes four plus
 * three rather than five plus two), only a few rows are on screen at a time and the list scrolls
 * with the mouse wheel. The page is driven by a single first-row index which is nudged every frame
 * so that the selected spell can never be scrolled out of sight.
 *
 * <p>The mixin extends the screen's own superclass because the platform only allows touching
 * members declared in the target class: scrolling has to be added as an override, and the screen
 * offset and width come from the superclass that way.
 */
@Mixin(value = InscriptionTableScreen.class, remap = false)
public abstract class InscriptionPaging extends AbstractContainerScreen<AbstractContainerMenu> {

    @Shadow(remap = false)
    protected ArrayList<?> spellSlots;

    @Shadow(remap = false)
    private int selectedSpellIndex;

    /** First row that is shown. */
    @Unique
    private int freeissui$page;

    /** Highest value {@link #freeissui$page} may take for the current book. */
    @Unique
    private int freeissui$lastPage;

    /** Cheap identity of the slot list, so a new spell book resets the page. */
    @Unique
    private int freeissui$slotIdentity;

    protected InscriptionPaging(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0 && freeissui$lastPage > 0 && freeissui$overViewport(mouseX, mouseY)) {
            int before = freeissui$page;
            freeissui$page += delta > 0.0 ? -1 : 1;
            freeissui$clampPage();
            if (before != freeissui$page) {
                freeissui$arrange();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Inject(method = "generateSpellSlots()V", at = @At("RETURN"), require = 1, remap = false)
    private void freeissui$arrangeFreshSlots(CallbackInfo callback) {
        freeissui$arrange();
    }

    @Inject(method = "renderSpells(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"),
            require = 1, remap = false)
    private void freeissui$clipToViewport(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback) {
        freeissui$arrange();
        int left = freeissui$viewportX();
        int top = freeissui$viewportY();
        graphics.enableScissor(left, top, left + DeckTuning.TABLE_BG_W, top + DeckTuning.TABLE_BG_H);
    }

    @Inject(method = "renderSpells(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("RETURN"),
            require = 1, remap = false)
    private void freeissui$releaseViewport(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback) {
        graphics.disableScissor();
        freeissui$drawScrollbar(graphics);
    }

    @Unique
    private void freeissui$arrange() {
        int total = spellSlots.size();
        if (total == 0) {
            freeissui$page = 0;
            freeissui$lastPage = 0;
            freeissui$slotIdentity = 0;
            return;
        }

        int identity = 31 * total + System.identityHashCode(freeissui$buttonAt(0));
        if (identity != freeissui$slotIdentity) {
            freeissui$slotIdentity = identity;
            freeissui$page = 0;
        }

        int rows = freeissui$rowCount(total);
        int[] rowSizes = freeissui$rowSizes(total, rows);
        freeissui$lastPage = Math.max(0, rows - DeckTuning.TABLE_ROWS);
        freeissui$followSelection(total, rows);
        freeissui$clampPage();

        float top = -(Math.min(rows, DeckTuning.TABLE_ROWS) * DeckTuning.TABLE_PITCH) / 2.0F;
        int slot = 0;
        for (int row = 0; row < rows; row++) {
            float left = -(rowSizes[row] * DeckTuning.TABLE_PITCH) / 2.0F;
            boolean onScreen = row >= freeissui$page && row < freeissui$page + DeckTuning.TABLE_ROWS;
            for (int column = 0; column < rowSizes[row]; column++) {
                SpellSlotInfoAccess slotInfo = (SpellSlotInfoAccess) spellSlots.get(slot++);
                slotInfo.freeissui$moveTo(new Vec2(
                        left + column * DeckTuning.TABLE_PITCH,
                        top + (row - freeissui$page) * DeckTuning.TABLE_PITCH));
                Button button = slotInfo.freeissui$button();
                button.visible = onScreen;
                button.active = onScreen;
            }
        }
    }

    @Unique
    private Button freeissui$buttonAt(int index) {
        return ((SpellSlotInfoAccess) spellSlots.get(index)).freeissui$button();
    }

    @Unique
    private int freeissui$rowCount(int total) {
        return (total + DeckTuning.TABLE_COLUMNS - 1) / DeckTuning.TABLE_COLUMNS;
    }

    /**
     * Row of a spell, proportional to how far it is in the book. This spreads the spells over the
     * rows so no row is left nearly empty.
     */
    @Unique
    private static int freeissui$rowOf(int index, int total, int rows) {
        return index * rows / total;
    }

    @Unique
    private int[] freeissui$rowSizes(int total, int rows) {
        int[] sizes = new int[rows];
        for (int index = 0; index < total; index++) {
            sizes[freeissui$rowOf(index, total, rows)]++;
        }
        return sizes;
    }

    @Unique
    private void freeissui$followSelection(int total, int rows) {
        if (selectedSpellIndex < 0 || selectedSpellIndex >= total) {
            return;
        }
        int row = freeissui$rowOf(selectedSpellIndex, total, rows);
        if (row < freeissui$page) {
            freeissui$page = row;
        } else if (row >= freeissui$page + DeckTuning.TABLE_ROWS) {
            freeissui$page = row - DeckTuning.TABLE_ROWS + 1;
        }
    }

    @Unique
    private void freeissui$clampPage() {
        freeissui$page = Math.max(0, Math.min(freeissui$page, freeissui$lastPage));
    }

    @Unique
    private void freeissui$drawScrollbar(GuiGraphics graphics) {
        if (freeissui$lastPage <= 0) {
            return;
        }
        int rows = freeissui$lastPage + DeckTuning.TABLE_ROWS;
        int left = freeissui$barX();
        int top = freeissui$viewportY();
        int thumbHeight = Math.max(8, DeckTuning.TABLE_BG_H / rows);
        int travel = DeckTuning.TABLE_BG_H - thumbHeight;
        int offset = Math.round(travel * (freeissui$page / (float) freeissui$lastPage));

        graphics.fill(left, top, left + DeckTuning.TABLE_BAR_W, top + DeckTuning.TABLE_BG_H,
                DeckTuning.TABLE_BAR_TRACK);
        graphics.fill(left, top + offset, left + DeckTuning.TABLE_BAR_W, top + offset + thumbHeight,
                DeckTuning.TABLE_BAR_THUMB);
    }

    /** The panel is centred when the GUI is wider than the background texture. */
    @Unique
    private int freeissui$panelShift() {
        return Math.max(0, imageWidth - DeckTuning.TABLE_PANEL_W + 1) / 2;
    }

    @Unique
    private int freeissui$viewportX() {
        return leftPos + DeckTuning.TABLE_BG_X + freeissui$panelShift();
    }

    @Unique
    private int freeissui$viewportY() {
        return topPos + DeckTuning.TABLE_BG_Y;
    }

    @Unique
    private int freeissui$barX() {
        return leftPos + DeckTuning.TABLE_BAR_X + freeissui$panelShift();
    }

    @Unique
    private boolean freeissui$overViewport(double mouseX, double mouseY) {
        int left = freeissui$viewportX();
        int top = freeissui$viewportY();
        return mouseX >= left && mouseX < freeissui$barX() + DeckTuning.TABLE_BAR_W
                && mouseY >= top && mouseY < top + DeckTuning.TABLE_BG_H;
    }
}
