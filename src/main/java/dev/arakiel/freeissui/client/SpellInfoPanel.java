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

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** The spell sheet that is printed beside the deck while Alt is held down. */
final class SpellInfoPanel {

    private SpellInfoPanel() {
    }

    static void paint(GuiGraphics graphics, float deckX, float centerY,
                      SpellSelectionManager.SelectionOption option, LocalPlayer player,
                      int position, int total) {
        SpellData data = option.spellData;
        if (data == null || data == SpellData.EMPTY) {
            return;
        }
        AbstractSpell spell = data.getSpell();
        int level = spell.getLevelFor(data.getLevel(), player);
        CooldownInstance cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spell.getSpellId());
        int remaining = cooldown == null ? 0 : cooldown.getCooldownRemaining();
        int full = cooldown == null ? spell.getSpellCooldown() : cooldown.getSpellCooldown();

        List<Component> lines = new ArrayList<>();
        lines.add(spell.getDisplayName(player));
        lines.add(Component.literal((position + 1) + " / " + total));
        lines.add(Component.translatable("freeissui.hud.level", level, spell.getMaxLevel()));
        lines.add(Component.translatable("freeissui.hud.rarity", spell.getRarity(level).getDisplayName()));
        lines.add(Component.translatable("freeissui.hud.mana", spell.getManaCost(level)));
        lines.add(Component.translatable("freeissui.hud.cooldown", seconds(remaining), seconds(full)));
        lines.add(Component.translatable("freeissui.hud.cast_time",
                seconds(spell.getEffectiveCastTime(level, player))));
        lines.add(Component.translatable("freeissui.hud.power",
                String.format(Locale.ROOT, "%.2f", spell.getSpellPower(level, player))));
        lines.add(Component.translatable("freeissui.hud.recast", spell.getRecastCount(level, player)));
        lines.addAll(spell.getUniqueInfo(level, player));

        Font font = Minecraft.getInstance().font;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(deckX + DeckTuning.SHEET_X, centerY + DeckTuning.SHEET_Y, 55.0F);
        pose.scale(DeckTuning.SHEET_SCALE, DeckTuning.SHEET_SCALE, 1.0F);
        for (int row = 0; row < lines.size(); row++) {
            graphics.drawString(font, lines.get(row), 0, row * DeckTuning.SHEET_LINE,
                    row == 0 ? DeckTuning.TEXT_TITLE : DeckTuning.TEXT_BODY, true);
        }
        pose.popPose();
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.2f", Math.max(0, ticks) / 20.0F);
    }
}
