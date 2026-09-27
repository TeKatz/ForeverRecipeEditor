package de.timo.foreverrecipeeditor.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class ForeverTheme {
    public static final int TEXT = 0xFFF4F1EA;
    public static final int MUTED_TEXT = 0xFFADB3C0;
    public static final int GOLD = 0xFFFFB84D;
    public static final int GOLD_SOFT = 0xFFD8963E;
    public static final int VIOLET = 0xFFA77BE8;
    public static final int VIOLET_SOFT = 0xFF7C5BA8;
    public static final int SUCCESS = 0xFF67D391;
    public static final int DANGER = 0xFFFF747C;
    public static final int PANEL = 0xF21D2029;
    public static final int PANEL_INNER = 0xFF252A35;
    public static final int CARD = 0xFF20242D;
    public static final int CARD_BORDER = 0xFF444B5A;
    public static final int SLOT = 0xFF181B22;
    public static final int SLOT_INNER = 0xFF303642;

    private ForeverTheme() {}

    public static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left + 5, top + 6, left + width + 5, top + height + 6, 0x70000000);
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0xFF0C0E13);
        graphics.fill(left, top, left + width, top + height, PANEL);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 28, 0xFF292432);
        graphics.fill(left + 1, top + 28, left + width - 1, top + 29, 0xFF4B4257);
        int split = left + width * 3 / 5;
        graphics.fill(left, top, split, top + 2, GOLD_SOFT);
        graphics.fill(split, top, left + width, top + 2, VIOLET_SOFT);
        graphics.fill(left + 3, top + 3, left + 4, top + height - 3, 0xFF343A47);
    }

    public static void drawCard(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, CARD_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, CARD);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0xFF303644);
    }

    public static void drawTextField(GuiGraphics graphics, int x, int y, int width, int height,
                                     boolean focused, boolean hovered, boolean editable) {
        int border = !editable ? 0xFF4B505B : focused ? GOLD : hovered ? VIOLET : CARD_BORDER;
        int background = editable ? 0xFF151820 : 0xFF20232A;
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
        if (focused && editable) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0xFF6B4B25);
        }
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF0E1015);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, SLOT_INNER);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, SLOT);
        graphics.fill(x + 2, y + 2, x + size - 2, y + 3, 0xFF3B4250);
    }

    public static void drawSlotHover(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0x78926AC7);
        graphics.fill(x - 1, y - 1, x + size + 1, y, VIOLET);
        graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, VIOLET);
        graphics.fill(x - 1, y, x, y + size, VIOLET);
        graphics.fill(x + size, y, x + size + 1, y + size, VIOLET);
    }

    public static void drawStatus(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF171A21);
        graphics.fill(x, y, x + 2, y + height, color);
        graphics.fill(x + 2, y, x + width, y + 1, 0xFF343A46);
    }

    public static void drawCenteredWrapped(GuiGraphics graphics, Font font, Component text, int centerX,
                                           int y, int maxWidth, int maxLines, int color) {
        var lines = font.split(text, maxWidth);
        int count = Math.min(maxLines, lines.size());
        for (int index = 0; index < count; index++) {
            graphics.drawCenteredString(font, lines.get(index), centerX, y + index * 9, color);
        }
    }
}
