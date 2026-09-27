package de.timo.foreverrecipeeditor.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.timo.foreverrecipeeditor.client.ClientState;
import de.timo.foreverrecipeeditor.network.NetworkHandler;
import de.timo.foreverrecipeeditor.network.NetworkHandler.ActionType;
import de.timo.foreverrecipeeditor.network.RecipeSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public final class RecipeEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 334;
    private static final int SLOT_SIZE = 18;

    private final Screen parent;
    private final IngredientSlot[] ingredients = new IngredientSlot[9];
    private final RecipeSnapshot initial;
    private final boolean canDelete;
    private ItemStack result;
    private boolean shaped;
    private EditBox recipeIdBox;
    private Button modeButton;
    private Button renameButton;
    private Button deleteButton;
    private Button saveButton;
    private String status = "";
    private boolean statusSuccess;
    private boolean actionInProgress;
    private ActionType pendingAction;

    public RecipeEditorScreen(Screen parent, RecipeSnapshot snapshot, boolean canDelete) {
        super(Component.translatable("screen.forever_recipe_editor.editor"));
        this.parent = parent;
        this.initial = snapshot;
        this.canDelete = canDelete;
        this.shaped = snapshot.shaped();
        this.result = snapshot.result().copy();
        for (int i = 0; i < ingredients.length; i++) {
            ingredients[i] = IngredientSlot.EMPTY;
        }
        loadSnapshot(snapshot);
    }

    private void loadSnapshot(RecipeSnapshot snapshot) {
        if (snapshot.shaped()) {
            for (int y = 0; y < snapshot.height(); y++) {
                for (int x = 0; x < snapshot.width(); x++) {
                    int source = y * snapshot.width() + x;
                    if (source < snapshot.ingredients().size() && x < 3 && y < 3) {
                        RecipeSnapshot.IngredientView view = snapshot.ingredients().get(source);
                        ingredients[y * 3 + x] = new IngredientSlot(view.json(), view.display().copy());
                    }
                }
            }
        } else {
            for (int i = 0; i < Math.min(9, snapshot.ingredients().size()); i++) {
                RecipeSnapshot.IngredientView view = snapshot.ingredients().get(i);
                ingredients[i] = new IngredientSlot(view.json(), view.display().copy());
            }
        }
    }

    @Override
    protected void init() {
        int left = getPanelLeft();
        int top = getPanelTop();
        recipeIdBox = new EditBox(font, left + 16, top + 46, PANEL_WIDTH - 32, 18,
                Component.translatable("screen.forever_recipe_editor.recipe_id"));
        recipeIdBox.setMaxLength(256);
        recipeIdBox.setValue(initial.id().toString());
        recipeIdBox.setEditable(!canDelete);
        recipeIdBox.setBordered(false);
        recipeIdBox.setTextColor(ForeverTheme.TEXT);
        recipeIdBox.setTextColorUneditable(ForeverTheme.MUTED_TEXT);
        if (canDelete) {
            recipeIdBox.setTooltip(Tooltip.create(Component.translatable(
                    "screen.forever_recipe_editor.recipe_id_existing_tooltip")));
        }
        addRenderableWidget(recipeIdBox);

        modeButton = addRenderableWidget(ForeverButton.create(modeLabel(), button -> {
            shaped = !shaped;
            modeButton.setMessage(modeLabel());
        }, ForeverButton.Style.ACCENT, left + 172, top + 81, 148, 18));

        addRenderableWidget(ForeverButton.create(Component.translatable("screen.forever_recipe_editor.clear"),
                button -> clearGrid(), ForeverButton.Style.SECONDARY, left + 172, top + 101, 148, 18));
        addRenderableWidget(ForeverButton.create(Component.translatable("screen.forever_recipe_editor.reset"),
                button -> reset(), ForeverButton.Style.SECONDARY, left + 172, top + 121, 148, 18));

        renameButton = ForeverButton.create(Component.translatable("screen.forever_recipe_editor.rename"),
                button -> openRenameScreen(), ForeverButton.Style.ACCENT,
                left + 172, top + 141, 148, 18).withTextOffset(1, 0);
        renameButton.active = canDelete;
        renameButton.setTooltip(Tooltip.create(Component.translatable(canDelete
                ? "screen.forever_recipe_editor.rename_tooltip"
                : "screen.forever_recipe_editor.rename_unavailable")));
        addRenderableWidget(renameButton);

        deleteButton = ForeverButton.create(Component.translatable("screen.forever_recipe_editor.delete"),
                button -> confirmDelete(), ForeverButton.Style.DANGER,
                left + 172, top + 161, 148, 18).withTextOffset(1, 0);
        deleteButton.active = canDelete;
        deleteButton.setTooltip(Tooltip.create(Component.translatable(canDelete
                ? "screen.forever_recipe_editor.delete_tooltip"
                : "screen.forever_recipe_editor.delete_unavailable")));
        addRenderableWidget(deleteButton);

        addRenderableWidget(ForeverButton.create(Component.translatable("gui.cancel"), button -> onClose(),
                ForeverButton.Style.SECONDARY, left + 12, top + 309, 96, 18));
        saveButton = ForeverButton.create(Component.translatable("screen.forever_recipe_editor.save"),
                button -> save(), ForeverButton.Style.PRIMARY, left + 114, top + 309, 214, 18);
        addRenderableWidget(saveButton);
        updateActionButtons();
    }

    private Component modeLabel() {
        return Component.translatable(shaped
                ? "screen.forever_recipe_editor.shaped"
                : "screen.forever_recipe_editor.shapeless");
    }

    private void clearGrid() {
        for (int i = 0; i < ingredients.length; i++) {
            ingredients[i] = IngredientSlot.EMPTY;
        }
        status = "";
    }

    private void reset() {
        clearGrid();
        shaped = initial.shaped();
        result = initial.result().copy();
        loadSnapshot(initial);
        recipeIdBox.setValue(initial.id().toString());
        modeButton.setMessage(modeLabel());
    }

    private void save() {
        if (actionInProgress) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(recipeIdBox.getValue().trim());
        if (id == null) {
            setStatus(false, "Invalid recipe ID.");
            return;
        }
        RecipeSnapshot.Edit edit = createEdit(id);
        if (edit == null) {
            return;
        }
        beginAction(ActionType.SAVE, "Saving and reloading…");
        NetworkHandler.save(edit);
    }

    private RecipeSnapshot.Edit createEdit(ResourceLocation id) {
        if (result.isEmpty()) {
            setStatus(false, "Place an item in the result slot.");
            return null;
        }
        List<String> ingredientJson = new ArrayList<>(9);
        boolean hasIngredient = false;
        for (IngredientSlot ingredient : ingredients) {
            ingredientJson.add(ingredient.json());
            hasIngredient |= !ingredient.json().isBlank();
        }
        if (!hasIngredient) {
            setStatus(false, "Place at least one ingredient in the grid.");
            return null;
        }
        return new RecipeSnapshot.Edit(id, shaped, ingredientJson, result.copy());
    }

    private void openRenameScreen() {
        if (canDelete && !actionInProgress) {
            minecraft.setScreen(new RecipeRenameScreen(this, initial.id()));
        }
    }

    public void renameTo(ResourceLocation newId) {
        if (!canDelete || actionInProgress) {
            return;
        }
        RecipeSnapshot.Edit edit = createEdit(newId);
        if (edit == null) {
            return;
        }
        beginAction(ActionType.RENAME, "Renaming and reloading…");
        NetworkHandler.rename(initial.id(), edit);
    }

    private void confirmDelete() {
        if (!canDelete || actionInProgress) {
            return;
        }
        minecraft.setScreen(new RecipeDeleteConfirmScreen(this, initial.id()));
    }

    public void deleteConfirmed() {
        if (!canDelete || actionInProgress) {
            return;
        }
        beginAction(ActionType.DELETE, "Deleting and reloading…");
        NetworkHandler.delete(initial.id());
    }

    public void setStatus(boolean success, String message) {
        this.statusSuccess = success;
        this.status = message;
    }

    private void beginAction(ActionType action, String message) {
        pendingAction = action;
        actionInProgress = true;
        ClientState.trackAction(this);
        setStatus(true, message);
        updateActionButtons();
    }

    private void updateActionButtons() {
        if (saveButton != null) {
            saveButton.active = !actionInProgress;
        }
        if (renameButton != null) {
            renameButton.active = canDelete && !actionInProgress;
        }
        if (deleteButton != null) {
            deleteButton.active = canDelete && !actionInProgress;
        }
    }

    public void handleActionResult(ActionType action, boolean success, boolean completed, String message) {
        if (pendingAction != null && pendingAction != action) {
            return;
        }
        setStatus(success, message);
        if (!completed) {
            return;
        }
        if (success) {
            if (parent instanceof RecipeSelectionScreen selection) {
                selection.refreshRecipes();
            } else {
                minecraft.setScreen(parent);
            }
            return;
        }
        pendingAction = null;
        actionInProgress = false;
        updateActionButtons();
    }

    public void setIngredient(int index, ItemStack stack) {
        if (index < 0 || index >= 9 || stack.isEmpty()) {
            return;
        }
        ItemStack display = stack.copyWithCount(1);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(display.getItem());
        ingredients[index] = new IngredientSlot("{\"item\":\"" + id + "\"}", display);
    }

    public void setResult(ItemStack stack) {
        if (!stack.isEmpty()) {
            this.result = stack.copyWithCount(Math.max(1, stack.getCount()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int craftSlot = craftingSlotAt(mouseX, mouseY);
        if (craftSlot >= 0) {
            if (button == 1) {
                if (hasShiftDown()) {
                    cycleTag(craftSlot);
                } else {
                    ingredients[craftSlot] = IngredientSlot.EMPTY;
                }
                return true;
            }
            if (button == 0) {
                ItemStack carried = getCarriedStack();
                if (!carried.isEmpty()) {
                    setIngredient(craftSlot, carried);
                }
                return true;
            }
        }

        if (getResultSlotArea().contains((int) mouseX, (int) mouseY)) {
            if (button == 1) {
                result = ItemStack.EMPTY;
                return true;
            }
            if (button == 0 && !getCarriedStack().isEmpty()) {
                setResult(getCarriedStack());
                return true;
            }
        }

        int inventoryIndex = inventorySlotAt(mouseX, mouseY);
        if (inventoryIndex >= 0 && (button == 0 || button == 1)) {
            return clickInventorySlot(inventoryIndex, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickInventorySlot(int inventoryIndex, int button) {
        if (minecraft.player == null || minecraft.gameMode == null) {
            return false;
        }
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        int menuSlot = findMenuSlot(menu, inventoryIndex);
        if (menuSlot < 0) {
            setStatus(false, "This inventory slot is not available in the current menu.");
            return true;
        }
        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, menuSlot, button, ClickType.PICKUP,
                minecraft.player);
        return true;
    }

    private int findMenuSlot(AbstractContainerMenu menu, int inventoryIndex) {
        if (minecraft.player == null) {
            return -1;
        }
        for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
            Slot slot = menu.slots.get(menuIndex);
            if (slot.container == minecraft.player.getInventory() && slot.getContainerSlot() == inventoryIndex) {
                return menuIndex;
            }
        }
        return -1;
    }

    private ItemStack getCarriedStack() {
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.containerMenu.getCarried();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!result.isEmpty() && getResultSlotArea().contains((int) mouseX, (int) mouseY)) {
            int count = Mth.clamp(result.getCount() + (scrollY > 0 ? 1 : -1), 1, result.getMaxStackSize());
            result.setCount(count);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void cycleTag(int slot) {
        IngredientSlot current = ingredients[slot];
        if (current.display().isEmpty()) {
            return;
        }
        List<ResourceLocation> tags = current.display().getTags()
                .map(tag -> tag.location())
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        if (tags.isEmpty()) {
            setStatus(false, "This item has no item tags.");
            return;
        }
        String currentTag = tagFromJson(current.json());
        if (currentTag == null) {
            setTag(slot, tags.getFirst());
            return;
        }
        int index = -1;
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).toString().equals(currentTag)) {
                index = i;
                break;
            }
        }
        if (index >= 0 && index + 1 < tags.size()) {
            setTag(slot, tags.get(index + 1));
        } else {
            setIngredient(slot, current.display());
        }
    }

    private void setTag(int slot, ResourceLocation tag) {
        ItemStack display = ingredients[slot].display();
        ingredients[slot] = new IngredientSlot("{\"tag\":\"" + tag + "\"}", display);
        setStatus(true, "Ingredient now uses #" + tag);
    }

    private static String tagFromJson(String json) {
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            return object.has("tag") ? object.get("tag").getAsString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = getPanelLeft();
        int top = getPanelTop();
        ForeverTheme.drawPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, width / 2, top + 10, ForeverTheme.GOLD);
        graphics.drawString(font, Component.translatable("screen.forever_recipe_editor.recipe_id"),
                left + 12, top + 34, ForeverTheme.MUTED_TEXT, false);
        if (canDelete) {
            Component locked = Component.translatable("screen.forever_recipe_editor.locked");
            graphics.drawString(font, locked, left + PANEL_WIDTH - 12 - font.width(locked), top + 34,
                    ForeverTheme.VIOLET, false);
        }
        ForeverTheme.drawTextField(graphics, left + 12, top + 43, PANEL_WIDTH - 24, 24,
                recipeIdBox.isFocused(), mouseX >= left + 12 && mouseX < left + PANEL_WIDTH - 12
                        && mouseY >= top + 43 && mouseY < top + 67, !canDelete);
        ForeverTheme.drawCard(graphics, left + 12, top + 75, 146, 108);
        ForeverTheme.drawCard(graphics, left + 164, top + 75, 164, 108);
        ForeverTheme.drawCard(graphics, left + 12, top + 189, 316, 97);

        graphics.drawString(font, Component.translatable("screen.forever_recipe_editor.ingredients"),
                left + 20, top + 82, ForeverTheme.TEXT, false);
        for (int index = 0; index < 9; index++) {
            Rect2i area = getCraftingSlotArea(index);
            drawSlot(graphics, area.getX(), area.getY(), ingredients[index].display(), false);
            if (tagFromJson(ingredients[index].json()) != null) {
                graphics.drawString(font, "#", area.getX() + 11, area.getY() + 9, 0x55FF55, true);
            }
            if (area.contains(mouseX, mouseY)) {
                ForeverTheme.drawSlotHover(graphics, area.getX(), area.getY(), SLOT_SIZE);
            }
        }

        int arrowX = left + 95;
        int arrowY = top + 117;
        graphics.drawString(font, "→", arrowX, arrowY, ForeverTheme.GOLD, false);
        Rect2i resultArea = getResultSlotArea();
        drawSlot(graphics, resultArea.getX(), resultArea.getY(), result, true, 1, 0);
        if (resultArea.contains(mouseX, mouseY)) {
            ForeverTheme.drawSlotHover(graphics, resultArea.getX(), resultArea.getY(), SLOT_SIZE);
        }

        graphics.drawString(font, Component.translatable("screen.forever_recipe_editor.inventory"),
                left + 20, top + 194, ForeverTheme.TEXT, false);
        renderInventory(graphics, mouseX, mouseY);
        if (!status.isBlank()) {
            int statusColor = actionInProgress ? ForeverTheme.GOLD
                    : statusSuccess ? ForeverTheme.SUCCESS : ForeverTheme.DANGER;
            ForeverTheme.drawStatus(graphics, left + 12, top + 289, 316, 16, statusColor);
            graphics.drawCenteredString(font, status, width / 2, top + 293, statusColor);
        }

        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }

        renderSlotTooltip(graphics, mouseX, mouseY);
        ItemStack carried = getCarriedStack();
        if (!carried.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 250.0F);
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            graphics.pose().popPose();
        }
    }

    private void renderSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = craftingSlotAt(mouseX, mouseY);
        if (index >= 0 && !ingredients[index].display().isEmpty()) {
            IngredientSlot ingredient = ingredients[index];
            List<Component> lines = new ArrayList<>();
            lines.add(ingredient.display().getHoverName());
            String tag = tagFromJson(ingredient.json());
            if (tag == null) {
                lines.add(Component.literal("Exact item").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal("#" + tag).withStyle(ChatFormatting.GREEN));
            }
            lines.add(Component.literal("Right-click: clear").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("Shift + right-click: cycle item/tags").withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderTooltip(font, lines, Optional.empty(), ingredient.display(), mouseX, mouseY);
            return;
        }
        if (getResultSlotArea().contains(mouseX, mouseY) && !result.isEmpty()) {
            List<Component> lines = List.of(
                    result.getHoverName(),
                    Component.literal("Mouse wheel: change amount").withStyle(ChatFormatting.DARK_GRAY),
                    Component.literal("Right-click: clear").withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderTooltip(font, lines, Optional.empty(), result, mouseX, mouseY);
            return;
        }
        int inventoryIndex = inventorySlotAt(mouseX, mouseY);
        if (inventoryIndex >= 0 && minecraft.player != null && getCarriedStack().isEmpty()) {
            ItemStack inventoryStack = minecraft.player.getInventory().getItem(inventoryIndex);
            if (!inventoryStack.isEmpty()) {
                List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, inventoryStack));
                lines.add(Component.translatable("screen.forever_recipe_editor.inventory_click_hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
                graphics.renderTooltip(font, lines, Optional.empty(), inventoryStack, mouseX, mouseY);
            }
        }
    }

    private void renderInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        if (minecraft.player == null) {
            return;
        }
        int left = inventoryLeft();
        int top = inventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventoryIndex = 9 + row * 9 + column;
                int x = left + column * SLOT_SIZE;
                int y = top + row * SLOT_SIZE;
                drawSlot(graphics, x, y, minecraft.player.getInventory().getItem(inventoryIndex), true);
                drawHoverHighlight(graphics, x, y, mouseX, mouseY);
            }
        }
        int hotbarTop = top + 58;
        for (int column = 0; column < 9; column++) {
            int x = left + column * SLOT_SIZE;
            drawSlot(graphics, x, hotbarTop, minecraft.player.getInventory().getItem(column), true);
            drawHoverHighlight(graphics, x, hotbarTop, mouseX, mouseY);
        }
    }

    private void drawHoverHighlight(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
            ForeverTheme.drawSlotHover(graphics, x, y, SLOT_SIZE);
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, ItemStack stack, boolean showCount) {
        drawSlot(graphics, x, y, stack, showCount, 0, 0);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, ItemStack stack, boolean showCount,
                          int itemOffsetX, int itemOffsetY) {
        ForeverTheme.drawSlot(graphics, x, y, SLOT_SIZE);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1 + itemOffsetX, y + 1 + itemOffsetY);
            if (showCount) {
                graphics.renderItemDecorations(font, stack, x + 1 + itemOffsetX, y + 1 + itemOffsetY);
            }
        }
    }

    private int craftingSlotAt(double mouseX, double mouseY) {
        for (int i = 0; i < 9; i++) {
            if (getCraftingSlotArea(i).contains((int) mouseX, (int) mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int left = inventoryLeft();
        int top = inventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                Rect2i area = new Rect2i(left + column * SLOT_SIZE, top + row * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
                if (area.contains((int) mouseX, (int) mouseY)) {
                    return 9 + row * 9 + column;
                }
            }
        }
        int hotbarTop = top + 58;
        for (int column = 0; column < 9; column++) {
            Rect2i area = new Rect2i(left + column * SLOT_SIZE, hotbarTop, SLOT_SIZE, SLOT_SIZE);
            if (area.contains((int) mouseX, (int) mouseY)) {
                return column;
            }
        }
        return -1;
    }

    public Rect2i getCraftingSlotArea(int index) {
        int x = getPanelLeft() + 24 + (index % 3) * SLOT_SIZE;
        int y = getPanelTop() + 100 + (index / 3) * SLOT_SIZE;
        return new Rect2i(x, y, SLOT_SIZE, SLOT_SIZE);
    }

    public Rect2i getResultSlotArea() {
        return new Rect2i(getPanelLeft() + 128, getPanelTop() + 118, SLOT_SIZE, SLOT_SIZE);
    }

    private int inventoryLeft() {
        return getPanelLeft() + 89;
    }

    private int inventoryTop() {
        return getPanelTop() + 205;
    }

    public int getPanelLeft() {
        return (width - PANEL_WIDTH) / 2;
    }

    public int getPanelTop() {
        return Math.max(4, (height - PANEL_HEIGHT) / 2);
    }

    public int getPanelWidth() {
        return PANEL_WIDTH;
    }

    public int getPanelHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    public void onClose() {
        ClientState.clearTrackedAction(this);
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record IngredientSlot(String json, ItemStack display) {
        private static final IngredientSlot EMPTY = new IngredientSlot("", ItemStack.EMPTY);
    }
}
