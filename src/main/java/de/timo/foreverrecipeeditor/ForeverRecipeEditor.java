package de.timo.foreverrecipeeditor;

import com.mojang.logging.LogUtils;
import de.timo.foreverrecipeeditor.network.NetworkHandler;
import de.timo.foreverrecipeeditor.recipe.GeneratedRecipePack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ForeverRecipeEditor.MOD_ID)
public final class ForeverRecipeEditor {
    public static final String MOD_ID = "forever_recipe_editor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ForeverRecipeEditor(IEventBus modBus, ModContainer container) {
        GeneratedRecipePack.initialize();
        modBus.addListener(GeneratedRecipePack::addPackFinder);
        modBus.addListener(NetworkHandler::register);
    }
}
