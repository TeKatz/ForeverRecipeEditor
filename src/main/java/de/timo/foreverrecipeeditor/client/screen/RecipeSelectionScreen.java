package de.timo.foreverrecipeeditor.client.screen;

import de.timo.foreverrecipeeditor.ForeverRecipeEditor;
import de.timo.foreverrecipeeditor.network.RecipeSnapshot;
import de.timo.foreverrecipeeditor.network.NetworkHandler;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class RecipeSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 310;
    private static final int VISIBLE_ROWS = 8;
    private final Screen parent;
    private final ItemStack target;
    private List<RecipeSnapshot> recipes;
    private int scroll;
    private boolean loading;

    public RecipeSelectionScreen(Screen parent, ItemStack target, List<RecipeSnapshot> recipes) {
        super(Component.translatable("screen.forever_recipe_editor.select"));
        this.parent = parent;
        this.target = target.copy();
        this.recipes = List.copyOf(recipes);
    }

    @Override
    protected void init() {
        int left = getPanelLeft() + 15;
        int top = getPanelTop();
        ForeverButton newRecipeButton = ForeverButton.create(
                Component.translatable("screen.forever_recipe_editor.new_recipe"), button -> {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(target.getItem());
            ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                    ForeverRecipeEditor.MOD_ID,
                    "generated/" + itemId.getNamespace() + "/" + itemId.getPath());
            minecraft.setScreen(new RecipeEditorScreen(this,
                    new RecipeSnapshot(recipeId, true, 3, 3, List.of(), target.copyWithCount(1)), false));
        }, ForeverButton.Style.PRIMARY, left, top + 62, 310, 22);
        newRecipeButton.active = !loading;
        addRenderableWidget(newRecipeButton);

        if (!loading) {
            int end = Math.min(recipes.size(), scroll + VISIBLE_ROWS);
            for (int index = scroll; index < end; index++) {
                RecipeSnapshot snapshot = recipes.get(index);
                int y = top + 91 + (index - scroll) * 23;
                String type = snapshot.shaped() ? "Shaped" : "Shapeless";
                Component label = Component.literal(snapshot.id() + "  [" + type + "]");
                ForeverButton recipeButton = ForeverButton.create(label,
                        button -> minecraft.setScreen(new RecipeEditorScreen(this, snapshot, true)),
                        ForeverButton.Style.SECONDARY, left, y, 310, 20);
                addRenderableWidget(recipeButton);
            }
        }

        addRenderableWidget(ForeverButton.create(Component.translatable("gui.cancel"), button -> onClose(),
                ForeverButton.Style.SECONDARY, left + 105, top + PANEL_HEIGHT - 28, 100, 20));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int old = scroll;
        if (scrollY < 0) {
            scroll = Math.min(Math.max(0, recipes.size() - VISIBLE_ROWS), scroll + 1);
        } else if (scrollY > 0) {
            scroll = Math.max(0, scroll - 1);
        }
        if (old != scroll) {
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = getPanelLeft();
        int top = getPanelTop();
        ForeverTheme.drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, width / 2, top + 10, ForeverTheme.GOLD);
        ForeverTheme.drawCard(graphics, left + 15, top + 34, PANEL_WIDTH - 30, 23);
        graphics.fill(left + 15, top + 34, left + 18, top + 57, ForeverTheme.VIOLET_SOFT);
        graphics.renderItem(target, left + 23, top + 37);
        graphics.drawString(font, target.getHoverName(), left + 45, top + 41, ForeverTheme.TEXT, false);
        if (loading) {
            ForeverTheme.drawStatus(graphics, left + 15, top + 91, PANEL_WIDTH - 30, 22, ForeverTheme.GOLD);
            graphics.drawCenteredString(font, Component.translatable("screen.forever_recipe_editor.refreshing"),
                    width / 2, top + 98, ForeverTheme.GOLD);
        } else if (recipes.isEmpty()) {
            ForeverTheme.drawStatus(graphics, left + 15, top + 91, PANEL_WIDTH - 30, 31, ForeverTheme.VIOLET);
            ForeverTheme.drawCenteredWrapped(graphics, font,
                    Component.translatable("screen.forever_recipe_editor.no_recipes"),
                    width / 2, top + 98, PANEL_WIDTH - 50, 2, ForeverTheme.MUTED_TEXT);
        }
        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    public boolean matches(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.getKey(target.getItem()).equals(itemId);
    }

    public void updateRecipes(List<RecipeSnapshot> updatedRecipes) {
        recipes = List.copyOf(updatedRecipes);
        scroll = Math.min(scroll, Math.max(0, recipes.size() - VISIBLE_ROWS));
        loading = false;
        rebuildWidgets();
    }

    public void refreshRecipes() {
        loading = true;
        minecraft.setScreen(this);
        NetworkHandler.requestRecipes(target);
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
