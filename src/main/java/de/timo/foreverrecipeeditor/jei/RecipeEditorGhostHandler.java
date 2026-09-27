package de.timo.foreverrecipeeditor.jei;

import de.timo.foreverrecipeeditor.client.screen.RecipeEditorScreen;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

public final class RecipeEditorGhostHandler implements IGhostIngredientHandler<RecipeEditorScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(RecipeEditorScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        if (!ingredient.getType().equals(VanillaTypes.ITEM_STACK)) {
            return List.of();
        }
        List<Target<I>> targets = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            final int slot = index;
            targets.add(target(screen.getCraftingSlotArea(index), value -> screen.setIngredient(slot, ((ItemStack) value).copy())));
        }
        targets.add(target(screen.getResultSlotArea(), value -> screen.setResult(((ItemStack) value).copy())));
        return targets;
    }

    private static <I> Target<I> target(Rect2i area, java.util.function.Consumer<I> consumer) {
        return new Target<>() {
            @Override
            public Rect2i getArea() {
                return area;
            }

            @Override
            public void accept(I ingredient) {
                consumer.accept(ingredient);
            }
        };
    }

    @Override
    public void onComplete() {}
}
