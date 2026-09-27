package de.timo.foreverrecipeeditor.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RecipeDeleteConfirmScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 150;

    private final RecipeEditorScreen parent;
    private final ResourceLocation recipeId;

    public RecipeDeleteConfirmScreen(RecipeEditorScreen parent, ResourceLocation recipeId) {
        super(Component.translatable("screen.forever_recipe_editor.delete_confirm_title"));
        this.parent = parent;
        this.recipeId = recipeId;
    }

    @Override
    protected void init() {
        int left = getPanelLeft();
        int top = getPanelTop();
        addRenderableWidget(ForeverButton.create(Component.translatable("gui.cancel"), button -> onClose(),
                ForeverButton.Style.SECONDARY, left + 14, top + 112, 108, 22));
        addRenderableWidget(ForeverButton.create(Component.translatable("screen.forever_recipe_editor.delete_yes"),
                button -> {
                    minecraft.setScreen(parent);
                    parent.deleteConfirmed();
                }, ForeverButton.Style.DANGER, left + 128, top + 112, 238, 22));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = getPanelLeft();
        int top = getPanelTop();
        ForeverTheme.drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, width / 2, top + 10, ForeverTheme.DANGER);
        ForeverTheme.drawCard(graphics, left + 14, top + 38, PANEL_WIDTH - 28, 60);
        graphics.fill(left + 14, top + 38, left + 17, top + 98, ForeverTheme.DANGER);
        ForeverTheme.drawCenteredWrapped(graphics, font,
                Component.translatable("screen.forever_recipe_editor.delete_confirm", recipeId),
                width / 2, top + 47, PANEL_WIDTH - 50, 3, ForeverTheme.TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.forever_recipe_editor.delete_warning"),
                width / 2, top + 73, ForeverTheme.MUTED_TEXT);
        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private int getPanelLeft() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int getPanelTop() {
        return Math.max(4, (height - PANEL_HEIGHT) / 2);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
