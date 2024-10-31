package net.lunade.copper.datagen.recipe;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.lunade.copper.registry.RegisterBlocks;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class SimpleCopperPipesRecipeProvider extends FabricRecipeProvider {
	public SimpleCopperPipesRecipeProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void buildRecipes(Consumer<FinishedRecipe> consumer) {
		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, RegisterBlocks.COPPER_PIPE, 3)
				.define('#', Items.COPPER_INGOT)
				.pattern("###")
				.pattern("   ")
				.pattern("###")
				.unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_COPPER_PIPE)
				.requires(RegisterBlocks.COPPER_PIPE)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.COPPER_PIPE), RecipeProvider.has(RegisterBlocks.COPPER_PIPE))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_EXPOSED_COPPER_PIPE)
				.requires(RegisterBlocks.EXPOSED_COPPER_PIPE)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.EXPOSED_COPPER_PIPE), RecipeProvider.has(RegisterBlocks.EXPOSED_COPPER_PIPE))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_WEATHERED_COPPER_PIPE)
				.requires(RegisterBlocks.WEATHERED_COPPER_PIPE)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.WEATHERED_COPPER_PIPE), RecipeProvider.has(RegisterBlocks.WEATHERED_COPPER_PIPE))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_OXIDIZED_COPPER_PIPE)
				.requires(RegisterBlocks.OXIDIZED_COPPER_PIPE)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.OXIDIZED_COPPER_PIPE), RecipeProvider.has(RegisterBlocks.OXIDIZED_COPPER_PIPE))
				.save(consumer);

		// FITTINGS

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, RegisterBlocks.COPPER_FITTING, 4)
				.define('#', Items.COPPER_INGOT)
				.pattern("###")
				.pattern("# #")
				.pattern("###")
				.unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_COPPER_FITTING)
				.requires(RegisterBlocks.COPPER_FITTING)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.COPPER_FITTING), RecipeProvider.has(RegisterBlocks.COPPER_FITTING))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_EXPOSED_COPPER_FITTING)
				.requires(RegisterBlocks.EXPOSED_COPPER_FITTING)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.EXPOSED_COPPER_FITTING), RecipeProvider.has(RegisterBlocks.EXPOSED_COPPER_FITTING))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_WEATHERED_COPPER_FITTING)
				.requires(RegisterBlocks.WEATHERED_COPPER_FITTING)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.WEATHERED_COPPER_FITTING), RecipeProvider.has(RegisterBlocks.WEATHERED_COPPER_FITTING))
				.save(consumer);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, RegisterBlocks.WAXED_OXIDIZED_COPPER_FITTING)
				.requires(RegisterBlocks.OXIDIZED_COPPER_FITTING)
				.requires(Items.HONEYCOMB)
				.unlockedBy(RecipeProvider.getHasName(RegisterBlocks.OXIDIZED_COPPER_FITTING), RecipeProvider.has(RegisterBlocks.OXIDIZED_COPPER_FITTING))
				.save(consumer);
	}
}