package de.timo.foreverrecipeeditor.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.timo.foreverrecipeeditor.ForeverRecipeEditor;
import de.timo.foreverrecipeeditor.client.screen.RecipeEditorScreen;
import de.timo.foreverrecipeeditor.client.screen.RecipeSelectionScreen;
import de.timo.foreverrecipeeditor.jei.ForeverJeiPlugin;
import de.timo.foreverrecipeeditor.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = ForeverRecipeEditor.MOD_ID, dist = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ClientMod {
    public static final KeyMapping EDIT_KEY = new KeyMapping(
            "key.forever_recipe_editor.edit",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.forever_recipe_editor");
    private static boolean scanCodeKeyDown;

    public ClientMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(ClientMod::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientMod::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(ClientMod::onKeyReleased);
        NeoForge.EVENT_BUS.addListener(ClientMod::onMousePressed);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(EDIT_KEY);
    }

    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (EDIT_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            scanCodeKeyDown = true;
        }
    }

    private static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (EDIT_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            scanCodeKeyDown = false;
        }
    }

    private static void onMousePressed(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || event.getAction() != InputConstants.PRESS
                || !isEditKeyDown()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null
                || minecraft.screen instanceof RecipeEditorScreen
                || minecraft.screen instanceof RecipeSelectionScreen) {
            return;
        }
        ItemStack hovered = ForeverJeiPlugin.getHoveredItem();
        if (hovered.isEmpty()) {
            return;
        }
        NetworkHandler.requestRecipes(hovered);
        event.setCanceled(true);
    }

    private static boolean isEditKeyDown() {
        InputConstants.Key key = EDIT_KEY.getKey();
        if (key == InputConstants.UNKNOWN) {
            return false;
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> scanCodeKeyDown;
        };
    }
}
