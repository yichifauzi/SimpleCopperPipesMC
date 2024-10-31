package net.lunade.copper.datagen.loot;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.lunade.copper.registry.RegisterBlocks;
import org.jetbrains.annotations.NotNull;

public final class SimpleCopperPipesBlockLootProvider extends FabricBlockLootTableProvider {

	public SimpleCopperPipesBlockLootProvider(@NotNull FabricDataOutput dataOutput) {
		super(dataOutput);
	}

	@Override
	public void generate() {
		this.dropSelf(RegisterBlocks.COPPER_PIPE);
		this.dropSelf(RegisterBlocks.EXPOSED_COPPER_PIPE);
		this.dropSelf(RegisterBlocks.WEATHERED_COPPER_PIPE);
		this.dropSelf(RegisterBlocks.OXIDIZED_COPPER_PIPE);

		this.dropSelf(RegisterBlocks.WAXED_COPPER_PIPE);
		this.dropSelf(RegisterBlocks.WAXED_EXPOSED_COPPER_PIPE);
		this.dropSelf(RegisterBlocks.WAXED_WEATHERED_COPPER_PIPE);
		this.dropSelf(RegisterBlocks.WAXED_OXIDIZED_COPPER_PIPE);

		this.dropSelf(RegisterBlocks.COPPER_FITTING);
		this.dropSelf(RegisterBlocks.EXPOSED_COPPER_FITTING);
		this.dropSelf(RegisterBlocks.WEATHERED_COPPER_FITTING);
		this.dropSelf(RegisterBlocks.OXIDIZED_COPPER_FITTING);

		this.dropSelf(RegisterBlocks.WAXED_COPPER_FITTING);
		this.dropSelf(RegisterBlocks.WAXED_EXPOSED_COPPER_FITTING);
		this.dropSelf(RegisterBlocks.WAXED_WEATHERED_COPPER_FITTING);
		this.dropSelf(RegisterBlocks.WAXED_OXIDIZED_COPPER_FITTING);
	}

}
