package de.timo.foreverrecipeeditor.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public record RecipeSnapshot(
        ResourceLocation id,
        boolean shaped,
        int width,
        int height,
        List<IngredientView> ingredients,
        ItemStack result) {
    private static final Gson GSON = new Gson();

    public static RecipeSnapshot from(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        boolean shaped = recipe instanceof ShapedRecipe;
        int width = shaped ? ((ShapedRecipe) recipe).getWidth() : 3;
        int height = shaped ? ((ShapedRecipe) recipe).getHeight() : 3;
        List<IngredientView> ingredients = recipe.getIngredients().stream()
                .map(ingredient -> IngredientView.from(ingredient, registries))
                .toList();
        return new RecipeSnapshot(holder.id(), shaped, width, height, ingredients,
                recipe.getResultItem(registries).copy());
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        ResourceLocation.STREAM_CODEC.encode(buffer, id);
        buffer.writeBoolean(shaped);
        buffer.writeVarInt(width);
        buffer.writeVarInt(height);
        buffer.writeVarInt(ingredients.size());
        ingredients.forEach(ingredient -> ingredient.write(buffer));
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, result);
    }

    public static RecipeSnapshot read(RegistryFriendlyByteBuf buffer) {
        ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buffer);
        boolean shaped = buffer.readBoolean();
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        int size = buffer.readVarInt();
        List<IngredientView> ingredients = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ingredients.add(IngredientView.read(buffer));
        }
        ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        return new RecipeSnapshot(id, shaped, width, height, ingredients, result);
    }

    public record IngredientView(String json, ItemStack display) {
        public static IngredientView from(Ingredient ingredient, HolderLookup.Provider registries) {
            JsonElement encoded = Ingredient.CODEC
                    .encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), ingredient)
                    .getOrThrow(error -> new IllegalArgumentException("Could not encode ingredient: " + error));
            ItemStack display = ingredient.getItems().length == 0
                    ? ItemStack.EMPTY
                    : ingredient.getItems()[0].copyWithCount(1);
            return new IngredientView(GSON.toJson(encoded), display);
        }

        public void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(json, 32767);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, display);
        }

        public static IngredientView read(RegistryFriendlyByteBuf buffer) {
            return new IngredientView(buffer.readUtf(32767), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }
    }

    public record Edit(ResourceLocation recipeId, boolean shaped, List<String> ingredients, ItemStack result) {
        public void write(RegistryFriendlyByteBuf buffer) {
            ResourceLocation.STREAM_CODEC.encode(buffer, recipeId);
            buffer.writeBoolean(shaped);
            buffer.writeVarInt(ingredients.size());
            ingredients.forEach(value -> buffer.writeUtf(value, 32767));
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, result);
        }

        public static Edit read(RegistryFriendlyByteBuf buffer) {
            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buffer);
            boolean shaped = buffer.readBoolean();
            int size = buffer.readVarInt();
            List<String> ingredients = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ingredients.add(buffer.readUtf(32767));
            }
            return new Edit(id, shaped, ingredients, ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }
    }
}
