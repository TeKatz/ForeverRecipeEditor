package de.timo.foreverrecipeeditor.network;

import de.timo.foreverrecipeeditor.ForeverRecipeEditor;
import de.timo.foreverrecipeeditor.client.ClientState;
import de.timo.foreverrecipeeditor.recipe.GeneratedRecipePack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NetworkHandler {
    private NetworkHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1.1");
        registrar.playToServer(RequestRecipes.TYPE, RequestRecipes.STREAM_CODEC, NetworkHandler::handleRequest);
        registrar.playToClient(RecipeList.TYPE, RecipeList.STREAM_CODEC, NetworkHandler::handleRecipeList);
        registrar.playToServer(SaveRecipe.TYPE, SaveRecipe.STREAM_CODEC, NetworkHandler::handleSave);
        registrar.playToServer(DeleteRecipe.TYPE, DeleteRecipe.STREAM_CODEC, NetworkHandler::handleDelete);
        registrar.playToServer(RenameRecipe.TYPE, RenameRecipe.STREAM_CODEC, NetworkHandler::handleRename);
        registrar.playToClient(ActionResult.TYPE, ActionResult.STREAM_CODEC, NetworkHandler::handleActionResult);
    }

    public static void requestRecipes(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        PacketDistributor.sendToServer(new RequestRecipes(id));
    }

    public static void save(RecipeSnapshot.Edit edit) {
        PacketDistributor.sendToServer(new SaveRecipe(edit));
    }

    public static void delete(ResourceLocation recipeId) {
        PacketDistributor.sendToServer(new DeleteRecipe(recipeId));
    }

    public static void rename(ResourceLocation oldRecipeId, RecipeSnapshot.Edit edit) {
        PacketDistributor.sendToServer(new RenameRecipe(oldRecipeId, edit));
    }

    private static void handleRequest(RequestRecipes payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Item requested = BuiltInRegistries.ITEM.get(payload.itemId());
        List<RecipeSnapshot> recipes = player.server.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value() instanceof ShapedRecipe || holder.value() instanceof ShapelessRecipe)
                .filter(holder -> holder.value().getResultItem(player.registryAccess()).is(requested))
                .map(holder -> RecipeSnapshot.from(holder, player.registryAccess()))
                .sorted(Comparator.comparing(snapshot -> snapshot.id().toString()))
                .toList();
        PacketDistributor.sendToPlayer(player, new RecipeList(payload.itemId(), recipes));
    }

    private static void handleRecipeList(RecipeList payload, IPayloadContext context) {
        ClientState.openRecipeList(payload.itemId(), payload.recipes());
    }

    private static void handleSave(SaveRecipe payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasPermissions(2)) {
            result(player, ActionType.SAVE, false, true, "You need operator permission to edit recipes.");
            return;
        }
        RecipeSnapshot.Edit edit = payload.edit();
        if (edit.ingredients().size() != 9) {
            result(player, ActionType.SAVE, false, true, "Invalid crafting grid size.");
            return;
        }
        try {
            GeneratedRecipePack.save(edit, player.registryAccess());
        } catch (Exception exception) {
            ForeverRecipeEditor.LOGGER.error("Could not save recipe {}", edit.recipeId(), exception);
            result(player, ActionType.SAVE, false, true, "Could not save recipe: " + exception.getMessage());
            return;
        }

        reload(player, ActionType.SAVE, "Recipe saved. Reloading data packs…", "Recipe reload complete.");
    }

    private static void handleDelete(DeleteRecipe payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasPermissions(2)) {
            result(player, ActionType.DELETE, false, true, "You need operator permission to delete recipes.");
            return;
        }
        try {
            GeneratedRecipePack.disable(payload.recipeId());
        } catch (Exception exception) {
            ForeverRecipeEditor.LOGGER.error("Could not delete recipe {}", payload.recipeId(), exception);
            result(player, ActionType.DELETE, false, true, "Could not delete recipe: " + exception.getMessage());
            return;
        }

        reload(player, ActionType.DELETE, "Recipe deleted. Reloading data packs…", "Recipe deletion complete.");
    }

    private static void handleRename(RenameRecipe payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasPermissions(2)) {
            result(player, ActionType.RENAME, false, true, "You need operator permission to rename recipes.");
            return;
        }
        RecipeSnapshot.Edit edit = payload.edit();
        if (payload.oldRecipeId().equals(edit.recipeId())) {
            result(player, ActionType.RENAME, false, true, "The new recipe ID must be different.");
            return;
        }
        if (edit.ingredients().size() != 9) {
            result(player, ActionType.RENAME, false, true, "Invalid crafting grid size.");
            return;
        }
        boolean oldExists = player.server.getRecipeManager().getRecipes().stream()
                .anyMatch(holder -> holder.id().equals(payload.oldRecipeId()));
        if (!oldExists) {
            result(player, ActionType.RENAME, false, true, "The original recipe no longer exists.");
            return;
        }
        boolean newExists = player.server.getRecipeManager().getRecipes().stream()
                .anyMatch(holder -> holder.id().equals(edit.recipeId()));
        if (newExists) {
            result(player, ActionType.RENAME, false, true, "A recipe with the new ID already exists.");
            return;
        }
        try {
            GeneratedRecipePack.rename(payload.oldRecipeId(), edit, player.registryAccess());
        } catch (Exception exception) {
            ForeverRecipeEditor.LOGGER.error("Could not rename recipe {} to {}",
                    payload.oldRecipeId(), edit.recipeId(), exception);
            result(player, ActionType.RENAME, false, true, "Could not rename recipe: " + exception.getMessage());
            return;
        }

        reload(player, ActionType.RENAME, "Recipe renamed. Reloading data packs…", "Recipe rename complete.");
    }

    private static void reload(ServerPlayer player, ActionType action, String startedMessage, String completedMessage) {
        result(player, action, true, false, startedMessage);
        var server = player.server;
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .whenComplete((ignored, throwable) -> server.execute(() -> {
                    if (throwable == null) {
                        if (player.connection.isAcceptingMessages()) {
                            result(player, action, true, true, completedMessage);
                        }
                    } else {
                        ForeverRecipeEditor.LOGGER.error("Recipe reload failed", throwable);
                        if (player.connection.isAcceptingMessages()) {
                            result(player, action, false, true,
                                    "Recipe was written, but reload failed. Check latest.log.");
                        }
                    }
                }));
    }

    private static void result(ServerPlayer player, ActionType action, boolean success, boolean completed,
                               String message) {
        PacketDistributor.sendToPlayer(player, new ActionResult(action, success, completed, message));
    }

    private static void handleActionResult(ActionResult payload, IPayloadContext context) {
        ClientState.showResult(payload.action(), payload.success(), payload.completed(), payload.message());
    }

    public record RequestRecipes(ResourceLocation itemId) implements CustomPacketPayload {
        public static final Type<RequestRecipes> TYPE = new Type<>(id("request_recipes"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestRecipes> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> ResourceLocation.STREAM_CODEC.encode(buffer, value.itemId),
                buffer -> new RequestRecipes(ResourceLocation.STREAM_CODEC.decode(buffer)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RecipeList(ResourceLocation itemId, List<RecipeSnapshot> recipes) implements CustomPacketPayload {
        public static final Type<RecipeList> TYPE = new Type<>(id("recipe_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeList> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> {
                    ResourceLocation.STREAM_CODEC.encode(buffer, value.itemId);
                    buffer.writeVarInt(value.recipes.size());
                    value.recipes.forEach(recipe -> recipe.write(buffer));
                },
                buffer -> {
                    ResourceLocation itemId = ResourceLocation.STREAM_CODEC.decode(buffer);
                    int size = buffer.readVarInt();
                    List<RecipeSnapshot> recipes = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        recipes.add(RecipeSnapshot.read(buffer));
                    }
                    return new RecipeList(itemId, recipes);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SaveRecipe(RecipeSnapshot.Edit edit) implements CustomPacketPayload {
        public static final Type<SaveRecipe> TYPE = new Type<>(id("save_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> value.edit.write(buffer),
                buffer -> new SaveRecipe(RecipeSnapshot.Edit.read(buffer)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DeleteRecipe(ResourceLocation recipeId) implements CustomPacketPayload {
        public static final Type<DeleteRecipe> TYPE = new Type<>(id("delete_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> ResourceLocation.STREAM_CODEC.encode(buffer, value.recipeId),
                buffer -> new DeleteRecipe(ResourceLocation.STREAM_CODEC.decode(buffer)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RenameRecipe(ResourceLocation oldRecipeId, RecipeSnapshot.Edit edit) implements CustomPacketPayload {
        public static final Type<RenameRecipe> TYPE = new Type<>(id("rename_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RenameRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> {
                    ResourceLocation.STREAM_CODEC.encode(buffer, value.oldRecipeId);
                    value.edit.write(buffer);
                },
                buffer -> new RenameRecipe(ResourceLocation.STREAM_CODEC.decode(buffer), RecipeSnapshot.Edit.read(buffer)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public enum ActionType {
        SAVE,
        DELETE,
        RENAME
    }

    public record ActionResult(ActionType action, boolean success, boolean completed, String message)
            implements CustomPacketPayload {
        public static final Type<ActionResult> TYPE = new Type<>(id("action_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionResult> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeUtf(value.action.name(), 16);
                    buffer.writeBoolean(value.success);
                    buffer.writeBoolean(value.completed);
                    buffer.writeUtf(value.message, 32767);
                },
                buffer -> new ActionResult(
                        ActionType.valueOf(buffer.readUtf(16)),
                        buffer.readBoolean(),
                        buffer.readBoolean(),
                        buffer.readUtf(32767)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ForeverRecipeEditor.MOD_ID, path);
    }
}
