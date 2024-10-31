package net.lunade.copper.registry;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Map;
import net.lunade.copper.blocks.block_entity.CopperPipeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PoweredPipeDispenses {
    private static final Map<ItemLike, PoweredDispense> ITEMS_TO_DISPENSES = new Object2ObjectLinkedOpenHashMap<>();

    public static void register(ItemLike item, PoweredDispense dispense) {
        ITEMS_TO_DISPENSES.put(item, dispense);
    }

    private static final PoweredDispense PROJECTILE_ITEM_DISPENSE = (world, stack, i, direction, position, state, pos, pipe) -> {
        if (stack.getItem() instanceof ProjectileItem projectileItem) {
            double d = position.x();
            double e = position.y();
            double f = position.z();
            Direction.Axis axis = direction.getAxis();
            e = getYOffset(axis, e);
            Projectile projectile = projectileItem.asProjectile(world, new Vec3(d, e, f), stack, direction);
            ProjectileItem.DispenseConfig config = projectileItem.createDispenseConfig();
            projectileItem
                    .shoot(
                            projectile,
                            direction.getStepX(),
                            direction.getStepY(),
                            direction.getStepZ(),
                            config.power(),
                            config.uncertainty()
                    );
            world.addFreshEntity(projectile);
        }
    };

    @Nullable
    public static PoweredDispense getDispense(ItemLike item) {
        if (ITEMS_TO_DISPENSES.containsKey(item)) {
            return ITEMS_TO_DISPENSES.get(item);
        } else if (item instanceof ProjectileItem) {
            return PROJECTILE_ITEM_DISPENSE;
        }
        return null;
    }

    public static double getYOffset(Direction.Axis axis, double e) {
        if (axis == Direction.Axis.Y) {
            return e - 0.125D;
        } else {
            return e - 0.15625D;
        }
    }

    public static double getRandom(@NotNull RandomSource random) {
        return (random.nextDouble() * 0.6D) - 0.3D;
    }

    public static double getVelX(Direction.Axis axis, int offX, int i) {
        return axis == Direction.Axis.X ? (i * offX) * 0.1D : 0D;
    }

    public static double getVelY(Direction.Axis axis, int offY, int i) {
        return axis == Direction.Axis.Y ? (i * offY) * 0.1D : 0D;
    }

    public static double getVelZ(Direction.Axis axis, int offZ, int i) {
        return axis == Direction.Axis.Z ? (i * offZ) * 0.1D : 0D;
    }

    public static void init() {
    }

    @FunctionalInterface
    public interface PoweredDispense {
        void dispense(ServerLevel world, ItemStack itemStack, int i, Direction direction, Position position, BlockState state, BlockPos pos, CopperPipeEntity pipe);
    }
}
