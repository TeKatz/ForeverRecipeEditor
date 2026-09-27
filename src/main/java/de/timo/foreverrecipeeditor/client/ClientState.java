package de.timo.foreverrecipeeditor.client;

import de.timo.foreverrecipeeditor.ForeverRecipeEditor;
import de.timo.foreverrecipeeditor.client.screen.RecipeEditorScreen;
import de.timo.foreverrecipeeditor.client.screen.RecipeSelectionScreen;
import de.timo.foreverrecipeeditor.network.RecipeSnapshot;
import de.timo.foreverrecipeeditor.network.NetworkHandler.ActionType;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ClientState {
    private static RecipeEditorScreen trackedEditor;

    private ClientState() {}

    public static void trackAction(RecipeEditorScreen editor) {
        trackedEditor = editor;
    }

    public static void clearTrackedAction(RecipeEditorScreen editor) {
        if (trackedEditor == editor) {
            trackedEditor = null;
        }
    }

    public static void openRecipeList(ResourceLocation itemId, List<RecipeSnapshot> recipes) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RecipeSelectionScreen selection && selection.matches(itemId)) {
            selection.updateRecipes(recipes);
            return;
        }
        ItemStack target = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
        minecraft.setScreen(new RecipeSelectionScreen(minecraft.screen, target, recipes));
    }

    public static void showResult(ActionType action, boolean success, boolean completed, String message) {
        Minecraft minecraft = Minecraft.getInstance();
        RecipeEditorScreen editor = minecraft.screen instanceof RecipeEditorScreen current
                ? current
                : trackedEditor;
        if (editor != null) {
            if (completed && !success && minecraft.screen != editor) {
                minecraft.setScreen(editor);
            }
            editor.handleActionResult(action, success, completed, message);
            if (completed) {
                clearTrackedAction(editor);
            }
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal("[Forever Recipe Editor] " + message),
                    false);
        }
        if (!success) {
            ForeverRecipeEditor.LOGGER.warn(message);
        }
    }
}
