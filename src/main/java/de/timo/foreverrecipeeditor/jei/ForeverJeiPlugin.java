package de.timo.foreverrecipeeditor.jei;

import de.timo.foreverrecipeeditor.ForeverRecipeEditor;
import de.timo.foreverrecipeeditor.client.screen.RecipeEditorScreen;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public final class ForeverJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ForeverRecipeEditor.MOD_ID, "jei");
    private static @Nullable IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(RecipeEditorScreen.class, ForeverJeiPlugin::createEditorProperties);
        registration.addGhostIngredientHandler(RecipeEditorScreen.class, new RecipeEditorGhostHandler());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static ItemStack getHoveredItem() {
        IJeiRuntime current = runtime;
        if (current == null) {
            return ItemStack.EMPTY;
        }
        Optional<ItemStack> listItem = current.getIngredientListOverlay()
                .getIngredientUnderMouse()
                .flatMap(typed -> typed.getItemStack());
        if (listItem.isPresent()) {
            return listItem.get().copy();
        }
        return current.getBookmarkOverlay()
                .getIngredientUnderMouse()
                .flatMap(typed -> typed.getItemStack())
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    private static EditorProperties createEditorProperties(RecipeEditorScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = screen.width > 1
                ? screen.width
                : Math.max(2, minecraft.getWindow().getGuiScaledWidth());
        int screenHeight = screen.height > 1
                ? screen.height
                : Math.max(2, minecraft.getWindow().getGuiScaledHeight());
        int guiLeft = (screenWidth - screen.getPanelWidth()) / 2;
        int guiTop = Math.max(4, (screenHeight - screen.getPanelHeight()) / 2);
        return new EditorProperties(RecipeEditorScreen.class, guiLeft, guiTop,
                screen.getPanelWidth(), screen.getPanelHeight(), screenWidth, screenHeight);
    }

    private record EditorProperties(
            Class<? extends net.minecraft.client.gui.screens.Screen> screenClass,
            int guiLeft,
            int guiTop,
            int guiXSize,
            int guiYSize,
            int screenWidth,
            int screenHeight) implements IGuiProperties {
    }
}
