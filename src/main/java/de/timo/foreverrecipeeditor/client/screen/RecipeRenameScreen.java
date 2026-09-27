package de.timo.foreverrecipeeditor.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RecipeRenameScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 164;

    private final RecipeEditorScreen parent;
    private final ResourceLocation oldId;
    private EditBox newIdBox;
    private Component error = Component.empty();

    public RecipeRenameScreen(RecipeEditorScreen parent, ResourceLocation oldId) {
        super(Component.translatable("screen.forever_recipe_editor.rename_title"));
        this.parent = parent;
        this.oldId = oldId;
    }

    @Override
    protected void init() {
        int left = getPanelLeft();
        int top = getPanelTop();
        newIdBox = new EditBox(font, left + 18, top + 78, PANEL_WIDTH - 36, 18,
                Component.translatable("screen.forever_recipe_editor.rename_new_id"));
        newIdBox.setMaxLength(256);
        newIdBox.setValue(oldId.toString());
        newIdBox.setBordered(false);
        newIdBox.setTextColor(ForeverTheme.TEXT);
        addRenderableWidget(newIdBox);
        setInitialFocus(newIdBox);

        addRenderableWidget(ForeverButton.create(Component.translatable("gui.cancel"), button -> onClose(),
                ForeverButton.Style.SECONDARY, left + 14, top + 130, 108, 20));
        addRenderableWidget(ForeverButton.create(Component.translatable("screen.forever_recipe_editor.rename_confirm"),
                button -> submit(), ForeverButton.Style.PRIMARY, left + 128, top + 130, 238, 20));
    }

    private void submit() {
        ResourceLocation newId = ResourceLocation.tryParse(newIdBox.getValue().trim());
        if (newId == null) {
            error = Component.translatable("screen.forever_recipe_editor.rename_invalid");
            return;
        }
        if (newId.equals(oldId)) {
            error = Component.translatable("screen.forever_recipe_editor.rename_same");
            return;
        }
        minecraft.setScreen(parent);
        parent.renameTo(newId);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = getPanelLeft();
        int top = getPanelTop();
        ForeverTheme.drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, width / 2, top + 10, ForeverTheme.VIOLET);
        ForeverTheme.drawCard(graphics, left + 14, top + 36, PANEL_WIDTH - 28, 23);
        graphics.fill(left + 14, top + 36, left + 17, top + 59, ForeverTheme.VIOLET_SOFT);
        graphics.drawString(font, Component.translatable("screen.forever_recipe_editor.rename_current", oldId),
                left + 23, top + 43, ForeverTheme.MUTED_TEXT, false);
        graphics.drawString(font, Component.translatable("screen.forever_recipe_editor.rename_new_id"),
                left + 14, top + 66, ForeverTheme.TEXT, false);
        ForeverTheme.drawTextField(graphics, left + 14, top + 75, PANEL_WIDTH - 28, 24,
                newIdBox.isFocused(), mouseX >= left + 14 && mouseX < left + PANEL_WIDTH - 14
                        && mouseY >= top + 75 && mouseY < top + 99, true);
        if (!error.getString().isBlank()) {
            ForeverTheme.drawStatus(graphics, left + 14, top + 104, PANEL_WIDTH - 28, 18, ForeverTheme.DANGER);
            graphics.drawCenteredString(font, error, width / 2, top + 109, ForeverTheme.DANGER);
        }
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
