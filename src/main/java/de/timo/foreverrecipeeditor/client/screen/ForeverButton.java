package de.timo.foreverrecipeeditor.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ForeverButton extends Button {
    public enum Style {
        PRIMARY,
        SECONDARY,
        ACCENT,
        DANGER
    }

    private final Style style;
    private int textOffsetX;
    private int textOffsetY;

    private ForeverButton(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public static ForeverButton create(Component message, OnPress onPress, Style style,
                                       int x, int y, int width, int height) {
        return new ForeverButton(x, y, width, height, message, onPress, style);
    }

    public ForeverButton withTextOffset(int x, int y) {
        textOffsetX = x;
        textOffsetY = y;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused();
        int border;
        int top;
        int bottom;
        int text;
        if (!active) {
            border = 0xFF3B3F49;
            top = 0xFF292C34;
            bottom = 0xFF22252C;
            text = 0xFF777C86;
        } else {
            switch (style) {
                case PRIMARY -> {
                    border = highlighted ? 0xFFFFD27A : 0xFFD0933D;
                    top = highlighted ? 0xFF805A27 : 0xFF62451F;
                    bottom = highlighted ? 0xFF62421C : 0xFF493317;
                    text = 0xFFFFF3DA;
                }
                case ACCENT -> {
                    border = highlighted ? 0xFFC6A2FF : 0xFF8362B4;
                    top = highlighted ? 0xFF604A80 : 0xFF493860;
                    bottom = highlighted ? 0xFF49365F : 0xFF372A49;
                    text = 0xFFF6ECFF;
                }
                case DANGER -> {
                    border = highlighted ? 0xFFFF9CA2 : 0xFFAC525A;
                    top = highlighted ? 0xFF71363C : 0xFF572C31;
                    bottom = highlighted ? 0xFF57292E : 0xFF402126;
                    text = 0xFFFFE7E8;
                }
                default -> {
                    border = highlighted ? 0xFF9AA4B5 : 0xFF596170;
                    top = highlighted ? 0xFF48505E : 0xFF373D48;
                    bottom = highlighted ? 0xFF383E49 : 0xFF2B3039;
                    text = ForeverTheme.TEXT;
                }
            }
        }

        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        graphics.fill(x + 2, y + height, x + width + 2, y + height + 2, 0x55000000);
        if (highlighted && active) {
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x50000000 | (border & 0x00FFFFFF));
        }
        graphics.fill(x, y, x + width, y + height, border);
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, top, bottom);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0x35FFFFFF);
        if (textOffsetX == 0 && textOffsetY == 0) {
            renderScrollingString(graphics, Minecraft.getInstance().font, 4, text);
        } else {
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    x + width / 2 + textOffsetX,
                    y + (height - 8) / 2 + textOffsetY,
                    text);
        }
    }
}
