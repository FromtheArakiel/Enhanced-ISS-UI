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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.gui.overlays.SpellBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Paints a composed deck: cards, selection halo, cooldown, focus frame and the step arrow.
 *
 * <p>The card atlas of Iron's Spells is looked up once, and the frame handed in by
 * {@link DeckView} is read, never allocated, so painting a frame stays free of garbage.
 */
final class CardPainter {

    private static final ResourceLocation ATLAS = SpellBarOverlay.TEXTURE;

    private CardPainter() {
    }

    static void paint(GuiGraphics graphics, DeckView.Frame frame,
                      List<SpellSelectionManager.SelectionOption> spells, DeckState state) {
        for (int i = 0; i < frame.count; i++) {
            DeckView.Card card = frame.cards.get(i);
            SpellSelectionManager.SelectionOption option = spells.get(card.index);

            if (card.focused && frame.haloAlpha > 0.001F) {
                halo(graphics, card, frame.haloAlpha);
            }
            card(graphics, option, card, card.focused ? 42.0F : 20.0F);
            if (card.focused) {
                cooldown(graphics, option, card);
                focusFrame(graphics, card, state);
                if (frame.arrow) {
                    arrow(graphics, card, frame.arrowDirection, frame.arrowStrength);
                }
            }
        }
    }

    private static void card(GuiGraphics graphics, SpellSelectionManager.SelectionOption option,
                             DeckView.Card card, float z) {
        SpellData data = option.spellData;
        if (data == null || data == SpellData.EMPTY || card.alpha <= 0.001F) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD_HALF, card.y + DeckTuning.CARD_HALF, z);
        pose.scale(card.zoom, card.zoom, 1.0F);
        pose.translate(-DeckTuning.CARD_HALF, -DeckTuning.CARD_HALF, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, card.alpha);

        graphics.blit(ATLAS, 0, 0, DeckTuning.CARD_U, DeckTuning.FRAME_V, DeckTuning.CARD, DeckTuning.CARD);
        if (!card.focused) {
            int frameU = Curios.SPELLBOOK_SLOT.equals(option.slot)
                    ? DeckTuning.FRAME_SPELLBOOK_U
                    : DeckTuning.FRAME_OTHER_U;
            graphics.blit(ATLAS, 0, 0, frameU, DeckTuning.FRAME_V, DeckTuning.CARD, DeckTuning.CARD);
        }
        graphics.blit(data.getSpell().getSpellIconResource(), 3, 3, 0.0F, 0.0F,
                DeckTuning.CARD_ICON, DeckTuning.CARD_ICON, DeckTuning.CARD_ICON, DeckTuning.CARD_ICON);
        graphics.flush();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    private static void halo(GuiGraphics graphics, DeckView.Card card, float emphasis) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD_HALF, card.y + DeckTuning.CARD_HALF, 18.0F);
        float spread = Mth.lerp(emphasis, 0.92F, 1.0F);
        pose.scale(spread, spread, 1.0F);
        int outer = shade(DeckTuning.HALO_OUTER_ALPHA, emphasis * card.alpha);
        int inner = shade(DeckTuning.HALO_INNER_ALPHA, emphasis * card.alpha);
        graphics.fill(-14, -14, 14, 14, outer << 24 | DeckTuning.HALO_OUTER);
        graphics.fill(-12, -12, 12, 12, inner << 24 | DeckTuning.HALO_INNER);
        pose.popPose();
    }

    private static void cooldown(GuiGraphics graphics, SpellSelectionManager.SelectionOption option,
                                 DeckView.Card card) {
        SpellData data = option.spellData;
        if (data == null || data == SpellData.EMPTY) {
            return;
        }
        float ratio = Mth.clamp(ClientMagicData.getCooldownPercent(data.getSpell()), 0.0F, 1.0F);
        if (ratio <= 0.0F) {
            return;
        }
        int filled = Mth.floor(DeckTuning.CARD_ICON * ratio);
        int alpha = shade(DeckTuning.COOLDOWN_ALPHA, card.alpha);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD_HALF, card.y + DeckTuning.CARD_HALF, 54.0F);
        pose.scale(card.zoom, card.zoom, 1.0F);
        pose.translate(-DeckTuning.CARD_HALF, -DeckTuning.CARD_HALF, 0.0F);
        graphics.fill(3, 19 - filled, 19, 19, alpha << 24 | DeckTuning.COOLDOWN);
        pose.popPose();
    }

    private static void focusFrame(GuiGraphics graphics, DeckView.Card card, DeckState state) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD_HALF, card.y + DeckTuning.CARD_HALF, 60.0F);
        float scale = card.zoom * state.pulseZoom();
        pose.scale(scale, scale, 1.0F);
        if (state.pulseActive()) {
            pose.mulPose(Axis.ZP.rotationDegrees(state.pulseSpin()));
        }
        pose.translate(-DeckTuning.CARD_HALF, -DeckTuning.CARD_HALF, 0.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, card.alpha);
        graphics.blit(ATLAS, 0, 0, DeckTuning.FRAME_FOCUSED_U, DeckTuning.FRAME_V,
                DeckTuning.CARD, DeckTuning.CARD);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    private static void arrow(GuiGraphics graphics, DeckView.Card card, int direction, float strength) {
        Font font = Minecraft.getInstance().font;
        Component glyph = Component.literal(direction < 0 ? "\u25B2" : "\u25BC");
        float swing = Mth.sin(Mth.clamp(strength, 0.0F, 1.0F) * (float) Math.PI);
        int alpha = Mth.clamp(Math.round((0.42F + swing * 0.58F) * card.alpha * 255.0F), 0, 255);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD + 2.0F, card.y + DeckTuning.CARD_HALF - 4.5F, 62.0F);
        float scale = 0.78F + swing * 0.14F;
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, glyph, 0, 0, alpha << 24 | DeckTuning.ARROW, true);
        pose.popPose();
    }

    private static int shade(int base, float factor) {
        return Mth.clamp(Math.round(base * factor), 0, 255);
    }
}
