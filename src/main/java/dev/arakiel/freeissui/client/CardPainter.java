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
 * <p>One instance is kept by the overlay so the tint bookkeeping survives between frames. The GUI
 * shader colour is only touched when the alpha actually changes, which means a folded deck - where
 * every visible card is opaque - is submitted as a single batch instead of one flush per card.
 * Fills and text are always drawn with the colour reset, so their own alpha stays exact.
 */
final class CardPainter {

    private static final ResourceLocation ATLAS = SpellBarOverlay.TEXTURE;
    private static final Component ARROW_UP = Component.literal("\u25B2");
    private static final Component ARROW_DOWN = Component.literal("\u25BC");

    /** Colour currently applied to the GUI shader, 1.0 meaning "no tint". */
    private float appliedTint = 1.0F;

    void paint(GuiGraphics graphics, DeckView.Frame frame,
               List<SpellSelectionManager.SelectionOption> spells, DeckState state) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // The scissor change in front of the deck already flushed the buffer, so this only costs a
        // call and guarantees a known starting point for the tint tracking below.
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        appliedTint = 1.0F;

        List<DeckView.Card> cards = frame.cards;
        float haloAlpha = frame.haloAlpha;
        for (int i = 0; i < frame.count; i++) {
            DeckView.Card card = cards.get(i);
            SpellSelectionManager.SelectionOption option = spells.get(card.index);
            boolean focused = card.focused;

            if (focused && haloAlpha > 0.001F) {
                noTint(graphics);
                halo(graphics, card, haloAlpha);
            }

            card(graphics, option, card, focused ? 42.0F : 20.0F);

            if (!focused) {
                continue;
            }
            noTint(graphics);
            cooldown(graphics, option, card);
            focusFrame(graphics, card, state);
            if (frame.arrow) {
                noTint(graphics);
                arrow(graphics, card, frame.arrowDirection, frame.arrowStrength);
            }
        }

        noTint(graphics);
    }

    /** Applies the wanted tint, flushing the previous batch only if it really differs. */
    private void tint(GuiGraphics graphics, float wanted) {
        float target = wanted >= 0.999F ? 1.0F : wanted;
        if (target == appliedTint) {
            return;
        }
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, target);
        appliedTint = target;
    }

    private void noTint(GuiGraphics graphics) {
        tint(graphics, 1.0F);
    }

    private void card(GuiGraphics graphics, SpellSelectionManager.SelectionOption option,
                      DeckView.Card card, float z) {
        SpellData data = option.spellData;
        if (data == null || data == SpellData.EMPTY || card.alpha <= 0.001F) {
            return;
        }
        tint(graphics, card.alpha);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD_HALF, card.y + DeckTuning.CARD_HALF, z);
        pose.scale(card.zoom, card.zoom, 1.0F);
        pose.translate(-DeckTuning.CARD_HALF, -DeckTuning.CARD_HALF, 0.0F);

        graphics.blit(ATLAS, 0, 0, DeckTuning.CARD_U, DeckTuning.FRAME_V, DeckTuning.CARD, DeckTuning.CARD);
        if (!card.focused) {
            String slot = option.slot;
            int frameU = slot != null && (slot == Curios.SPELLBOOK_SLOT || Curios.SPELLBOOK_SLOT.equals(slot))
                    ? DeckTuning.FRAME_SPELLBOOK_U
                    : DeckTuning.FRAME_OTHER_U;
            graphics.blit(ATLAS, 0, 0, frameU, DeckTuning.FRAME_V, DeckTuning.CARD, DeckTuning.CARD);
        }
        graphics.blit(data.getSpell().getSpellIconResource(), 3, 3, 0.0F, 0.0F,
                DeckTuning.CARD_ICON, DeckTuning.CARD_ICON, DeckTuning.CARD_ICON, DeckTuning.CARD_ICON);
        pose.popPose();
    }

    private void halo(GuiGraphics graphics, DeckView.Card card, float emphasis) {
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

    private void cooldown(GuiGraphics graphics, SpellSelectionManager.SelectionOption option,
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

    private void focusFrame(GuiGraphics graphics, DeckView.Card card, DeckState state) {
        tint(graphics, card.alpha);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD_HALF, card.y + DeckTuning.CARD_HALF, 60.0F);
        float scale = card.zoom * state.pulseZoom();
        pose.scale(scale, scale, 1.0F);
        if (state.pulseActive()) {
            pose.mulPose(Axis.ZP.rotationDegrees(state.pulseSpin()));
        }
        pose.translate(-DeckTuning.CARD_HALF, -DeckTuning.CARD_HALF, 0.0F);
        graphics.blit(ATLAS, 0, 0, DeckTuning.FRAME_FOCUSED_U, DeckTuning.FRAME_V,
                DeckTuning.CARD, DeckTuning.CARD);
        pose.popPose();
    }

    private void arrow(GuiGraphics graphics, DeckView.Card card, int direction, float strength) {
        Component glyph = direction < 0 ? ARROW_UP : ARROW_DOWN;
        float swing = Mth.sin(Mth.clamp(strength, 0.0F, 1.0F) * (float) Math.PI);
        int alpha = Mth.clamp(Math.round((0.42F + swing * 0.58F) * card.alpha * 255.0F), 0, 255);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(card.x + DeckTuning.CARD + 2.0F, card.y + DeckTuning.CARD_HALF - 4.5F, 62.0F);
        float scale = 0.78F + swing * 0.14F;
        pose.scale(scale, scale, 1.0F);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, glyph, 0, 0, alpha << 24 | DeckTuning.ARROW, true);
        pose.popPose();
    }

    private static int shade(int base, float factor) {
        return Mth.clamp(Math.round(base * factor), 0, 255);
    }
}
