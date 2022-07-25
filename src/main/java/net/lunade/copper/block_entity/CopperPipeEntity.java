package net.lunade.copper.block_entity;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.lunade.copper.Main;
import net.lunade.copper.blocks.CopperFitting;
import net.lunade.copper.blocks.CopperPipe;
import net.lunade.copper.pipe_nbt.ExtraPipeData;
import net.lunade.copper.pipe_nbt.MoveablePipeDataHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.GameEventTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.event.listener.VibrationListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class CopperPipeEntity extends AbstractSimpleCopperBlockEntity implements VibrationListener.Callback {

    public int transferCooldown;
    public int dispenseCooldown;
    public int noteBlockCooldown;

    private CopperPipeListener listener;
    public ExtraPipeData extraPipeData;

    public CopperPipeEntity(BlockPos blockPos, BlockState blockState) {
        super(Main.COPPER_PIPE_ENTITY, blockPos, blockState, MoveablePipeDataHandler.MOVE_TYPE.FROM_PIPE);
        this.noteBlockCooldown = 0;
        this.listener = new CopperPipeListener(new BlockPositionSource(this.pos), 8, this, null, 0,0);
        this.extraPipeData = null;
    }

    public void setStack(int i, ItemStack itemStack) {
        this.checkLootInteraction(null);
        this.getInvStackList().set(i, itemStack);
        if (itemStack.getCount() > this.getMaxCountPerStack()) {
            itemStack.setCount(this.getMaxCountPerStack());
        }
    }

    public void serverTick(World world, BlockPos blockPos, BlockState blockState) {
        this.listener.tick(world);
        super.serverTick(world, blockPos, blockState);
        blockState = world.getBlockState(blockPos);
        if (!world.isClient) {
            if (this.noteBlockCooldown>0) { --this.noteBlockCooldown; }
            if (this.dispenseCooldown>0) {
                --this.dispenseCooldown;
            } else {
                dispense((ServerWorld) world, blockPos, blockState, this);
                int i = 0;
                if (world.getBlockState(blockPos.offset(blockState.get(Properties.FACING).getOpposite())).getBlock() instanceof CopperFitting fitting) {
                    i = fitting.cooldown;
                } else {
                    if (blockState.getBlock() instanceof CopperPipe pipe) {
                        i = MathHelper.floor(pipe.cooldown*0.5);
                    }
                }
                this.dispenseCooldown=i;
            }
        }

        if (this.transferCooldown>0) {
            --this.transferCooldown;
        } else {
            pipeMove(world, blockPos, blockState, this);
        }
    }

    public static void pipeMove(World world, BlockPos blockPos, BlockState blockState, CopperPipeEntity copperPipeEntity) {
        if (!world.isClient) {
            boolean bl1 = moveOut(world, blockPos, blockState, copperPipeEntity);
            int bl2 = moveIn(world, blockPos, blockState, copperPipeEntity, copperPipeEntity);
            if (bl1 || bl2 >= 2) {
                markDirty(world, blockPos, blockState);
            }
            if (bl2 == 1 || bl2 == 3) {
                world.playSound(null, blockPos, Main.ITEM_IN, SoundCategory.BLOCKS, 0.2F, (world.random.nextFloat() * 0.25F) + 0.8F);
            }
        }
    }

    public static boolean canTransfer(World world, BlockPos pos, boolean out) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity != null) {
            if (entity instanceof CopperPipeEntity pipe) { return pipe.transferCooldown<=0; }
            if (entity instanceof CopperFittingEntity) { return out || !world.getBlockState(pos).get(Properties.POWERED); }
        } return true;
    }

    private static int moveIn(World world, BlockPos blockPos, BlockState blockState, Inventory inventory, CopperPipeEntity pipe) {
        Inventory inventory2 = getSecretInventory(world, blockPos, blockState);
        if (inventory2 != null) {
            Direction direction = blockState.get(Properties.FACING);
            if (!isInventoryFull(inventory, direction) && canTransfer(world, blockPos.offset(direction.getOpposite()), false)) {
                for (int i = 0; i < inventory2.size(); ++i) {
                    if (!inventory2.getStack(i).isEmpty()) {
                        pipe.setCooldown(blockState);
                        ItemStack itemStack = inventory2.getStack(i).copy();
                        ItemStack itemStack2 = transfer(inventory, inventory2.removeStack(i, 1), direction);
                        Block block = world.getBlockState(blockPos.offset(direction.getOpposite())).getBlock();
                        if (itemStack2.isEmpty()) {
                            inventory.markDirty();
                            if (!(block instanceof CopperPipe) && !(block instanceof CopperFitting)) {return 3;}
                            return 2;
                        }
                        inventory2.setStack(i, itemStack);
                    }
                }
            }
        } return 0;
    }

    private static boolean moveOut(World world, BlockPos blockPos, BlockState blockState, Inventory inventory) {
        Inventory inventory2 = getOutputInventory(world, blockPos, blockState);
        Direction direction = blockState.get(Properties.FACING);
        if (inventory2 != null && canTransfer(world, blockPos.offset(direction), true)) {
            direction = direction.getOpposite();
            if (!isPipe(world, blockPos, direction) && !isInventoryFull(inventory2, direction)) {
                for (int i = 0; i < inventory.size(); ++i) {
                    if (!inventory.getStack(i).isEmpty()) {
                        setCooldown(world, blockPos.offset(direction.getOpposite()));
                        ItemStack itemStack = inventory.getStack(i).copy();
                        ItemStack itemStack2 = transfer(inventory2, inventory.removeStack(i, 1), direction);
                        if (itemStack2.isEmpty()) {
                            inventory2.markDirty();
                            return true;
                        }
                        inventory.setStack(i, itemStack);
                    }
                }

            }
        } return false;
    }

    private static boolean dispense(ServerWorld serverWorld, BlockPos blockPos, BlockState blockState, CopperPipeEntity entity) {
        Direction direction = blockState.get(Properties.FACING);
        Direction directionOpp = direction.getOpposite();
        Block dirBlock = serverWorld.getBlockState(blockPos.offset(direction)).getBlock();
        Block oppBlock = serverWorld.getBlockState(blockPos.offset(directionOpp)).getBlock();
        boolean bl1 = dirBlock == Blocks.AIR;
        boolean bl2 = oppBlock != Blocks.AIR;
        boolean bl3 = dirBlock == Blocks.WATER;
        boolean bl4 = oppBlock != Blocks.WATER;
        boolean powered = blockState.get(CopperPipe.POWERED);
        boolean shootsSpecial = oppBlock == Blocks.DISPENSER;
        boolean shootsControlled = oppBlock == Blocks.DROPPER;
        boolean corroded = oppBlock == CopperFitting.CORRODED_FITTING || blockState.getBlock() == CopperPipe.CORRODED_PIPE;
        if ((bl1 || bl3) && (bl2 && bl4)) {
            BlockPointerImpl blockPointerImpl = new BlockPointerImpl(serverWorld, blockPos);
            CopperPipeEntity copperPipeEntity = blockPointerImpl.getBlockEntity();
            int i = copperPipeEntity.chooseNonEmptySlot(serverWorld.random);
            if (!(i < 0)) {
                ItemStack itemStack = copperPipeEntity.getStack(i);
                if (!itemStack.isEmpty()) {
                    ItemStack itemStack2;
                    int o=4;
                    if (shootsControlled) { //If Dropper
                        o=10;
                        serverWorld.playSound(null, blockPos, Main.LAUNCH, SoundCategory.BLOCKS, 0.2F, (serverWorld.random.nextFloat()*0.25F) + 0.8F);
                    } else if (shootsSpecial) { //If Dispenser, Use Pipe-Specific Launch Length
                        if (blockState.getBlock() instanceof CopperPipe pipe) {
                            o = pipe.dispenserShotLength;
                            serverWorld.playSound(null, blockPos, Main.LAUNCH, SoundCategory.BLOCKS, 0.2F, (serverWorld.random.nextFloat()*0.25F) + 0.8F);
                        } else {o=12;}
                    }
                    if (serverWorld.getBlockState(blockPos.offset(directionOpp)).getBlock() instanceof CopperFitting) {
                        itemStack2 = canonShoot(blockPointerImpl, itemStack, blockState, o, powered, true, corroded, entity);
                    } else {
                        itemStack2 = canonShoot(blockPointerImpl, itemStack, blockState, o, powered, false, corroded, entity);
                        blockPointerImpl.getWorld().syncWorldEvent(2000, blockPointerImpl.getPos(), direction.getId());
                    }
                    copperPipeEntity.setStack(i, itemStack2);
                    return true;
                }
            }
        }
        return false;
    }

    private static ItemStack canonShoot(BlockPointer blockPointer, ItemStack itemStack, BlockState state, int i, boolean powered, boolean fitting, boolean corroded, CopperPipeEntity entity) {
        ServerWorld world = blockPointer.getWorld();
        BlockPos pos = blockPointer.getPos();
        Direction direction = blockPointer.getBlockState().get(Properties.FACING);
        Position position = CopperPipe.getOutputLocation(blockPointer);
        ItemStack itemStack2 = itemStack;
        if (powered) { //Special Behavior When Powered
            if (itemStack2.isOf(Items.TIPPED_ARROW) || itemStack2.isOf(Items.SPECTRAL_ARROW) || itemStack2.isOf(Items.ARROW) || itemStack2.isOf(Items.SNOWBALL) ||
                    itemStack2.isOf(Items.EGG) || itemStack2.isOf(Items.EXPERIENCE_BOTTLE) || itemStack2.isOf(Items.SPLASH_POTION) || itemStack2.isOf(Items.LINGERING_POTION) || itemStack2.isOf(Items.FIRE_CHARGE)) {
                itemStack2=itemStack.split(1);
                spawnThrowable(world, itemStack2, i, direction, position, state, corroded, pos, entity);
                if (!fitting) {world.playSound(null, pos, Main.ITEM_OUT, SoundCategory.BLOCKS, 0.2F, (world.random.nextFloat()*0.25F) + 0.8F);}
                return itemStack;
            }
        }
        if (fitting) {
            if (itemStack2.isOf(Items.GLOW_INK_SAC) || itemStack2.isOf(Items.INK_SAC) || itemStack2.isOf(Items.SCULK_SENSOR)) { //Particle Emitters With Fitting
                spawnThrowable(world, itemStack2, i, direction, position, state, corroded, pos, entity);
            } else { //Spawn Item W/O Sound With Fitting
                itemStack2=itemStack.split(1);
                spawnItem(world, itemStack2, i, direction, position, state, corroded);
                world.syncWorldEvent(2000, pos, state.get(Properties.FACING).getId());
            }
            return itemStack;
        } else {
            itemStack2=itemStack.split(1);
            blockPointer.getWorld().syncWorldEvent(2000, blockPointer.getPos(), state.get(Properties.FACING).getId());
            spawnItem(blockPointer.getWorld(), itemStack2, i, direction, position, state, corroded);
            blockPointer.getWorld().playSound(null, blockPointer.getPos(), Main.ITEM_OUT, SoundCategory.BLOCKS, 0.2F, (world.random.nextFloat()*0.25F) + 0.8F);
            return itemStack;
        }
    }

    public static void spawnItem(World world, ItemStack itemStack, int i, Direction direction, Position position, BlockState state, boolean corroded) { //Simply Spawn An Item
        double d = position.getX();
        double e = position.getY();
        double f = position.getZ();
        if (direction.getAxis() == Direction.Axis.Y) {
            e -= 0.125D;
        } else {
            e -= 0.15625D;
        }
        double x = 0;
        double y = 0;
        double z = 0;
        Direction.Axis axis = state.get(Properties.FACING).getAxis();
        x = axis == Direction.Axis.X ? (i * state.get(Properties.FACING).getOffsetX()) * 0.1 : corroded ? (world.random.nextDouble()*0.6) - 0.3 : x;
        y = axis == Direction.Axis.Y ? (i * state.get(Properties.FACING).getOffsetY()) * 0.1 : corroded ? (world.random.nextDouble()*0.6) - 0.3 : y;
        z = axis == Direction.Axis.Z ? (i * state.get(Properties.FACING).getOffsetZ()) * 0.1 : corroded ? (world.random.nextDouble()*0.6) - 0.3 : z;
        ItemEntity itemEntity = new ItemEntity(world, d, e, f, itemStack);
        itemEntity.setVelocity(x, y, z);
        world.spawnEntity(itemEntity);
    }
    public static void spawnThrowable(ServerWorld world, ItemStack itemStack, int i, Direction direction, Position position, BlockState state, boolean corroded, BlockPos pos, CopperPipeEntity entity) {
        double d = position.getX();
        double e = position.getY();
        double f = position.getZ();
        if (direction.getAxis() == Direction.Axis.Y) { e -= 0.125D;} else { e -= 0.15625D; }
        double velX = 0;
        double velY = 0;
        double velZ = 0;
        Random random = world.random;
        double random1 = (random.nextDouble()*0.6) - 0.3;
        double random2 = (random.nextDouble()*0.6) - 0.3;
        Entity shotEntity = null;
        Direction dir = state.get(Properties.FACING);
        Direction.Axis axis = dir.getAxis();
        int offX = dir.getOffsetX();
        int offY = dir.getOffsetY();
        int offZ = dir.getOffsetZ();
        velX = axis == Direction.Axis.X ? (i * offX) * 0.1 : corroded ? (axis == Direction.Axis.Z ? random2 : random1) : velX;
        velY = axis == Direction.Axis.Y ? (i * offY) * 0.1 : corroded ? random1 : velY;
        velZ = axis == Direction.Axis.Z ? (i * offZ) * 0.1 : corroded ? random2 : velZ;
        if (itemStack.isIn(ItemTags.ARROWS) && !itemStack.isOf(Items.TIPPED_ARROW) && !itemStack.isOf(Items.SPECTRAL_ARROW)) {
            shotEntity = new ArrowEntity(world,d,e,f);
            shotEntity.setPos(d, e, f);
            ((ArrowEntity)shotEntity).pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        }
        if (itemStack.isOf(Items.SPECTRAL_ARROW)) {
            shotEntity = new SpectralArrowEntity(world,d,e,f);
            shotEntity.setPos(d, e, f);
            ((SpectralArrowEntity)shotEntity).pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        }
        if (itemStack.isOf(Items.TIPPED_ARROW)) {
            shotEntity = new ArrowEntity(world,d,e,f);
            ((ArrowEntity)shotEntity).initFromStack(itemStack);
            ((ArrowEntity)shotEntity).pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        }
        if (itemStack.isOf(Items.SNOWBALL)) {
            shotEntity = new SnowballEntity(world,d,e,f);
        }
        if (itemStack.isOf(Items.EGG)) {
            shotEntity = new EggEntity(world,d,e,f);
        }
        if (itemStack.isOf(Items.EXPERIENCE_BOTTLE)) {
            shotEntity = new ExperienceBottleEntity(world,d,e,f);
        }
        if (itemStack.isOf(Items.SPLASH_POTION)) {
            shotEntity = Util.make(new PotionEntity(world, d, e, f), (potionEntity) -> potionEntity.setItem(itemStack));
        }
        if (itemStack.isOf(Items.LINGERING_POTION)) {
            shotEntity = Util.make(new PotionEntity(world, d, e, f), (potionEntity) -> potionEntity.setItem(itemStack));        }
        if (itemStack.isOf(Items.FIRE_CHARGE)) {
            SmallFireballEntity smallFireballEntity = new SmallFireballEntity(world, d, e, f, velX, velY, velZ);
            world.spawnEntity(Util.make(smallFireballEntity, (smallFireballEntityx) -> smallFireballEntityx.setItem(itemStack)));
        }
        if (shotEntity!=null) {
            shotEntity.setVelocity(velX, velY, velZ);
            world.spawnEntity(shotEntity);
        }
        //PARTICLES
        random1 = (random.nextDouble()*7) - 3.5;
        random2 = (random.nextDouble()*7) - 3.5;
        velX = axis == Direction.Axis.X ? (i * offX) * 2 : (axis==Direction.Axis.Z ? corroded ? random2 : random2*0.1 : corroded ? random1 : random1*0.1);
        velY = axis == Direction.Axis.Y ? (i * offY) * 2 : corroded ? random1 : random1*0.1;
        velZ = axis == Direction.Axis.Z ? (i * offZ) * 2 : corroded ? random2 : random2*0.1;

        UniformIntProvider ran1 = UniformIntProvider.create(-3,3);
        UniformIntProvider ran2 = UniformIntProvider.create(-1,1);
        UniformIntProvider ran3 = UniformIntProvider.create(-3,3);
        boolean genericInkSac = itemStack.isOf(Items.INK_SAC);
        if (genericInkSac || itemStack.isOf(Items.GLOW_INK_SAC)) {
            if (state.getBlock() instanceof CopperPipe pipe) {
                ParticleEffect ink = genericInkSac ? pipe.ink : ParticleTypes.SQUID_INK;
                if (world.getBlockState(pos.offset(state.get(Properties.FACING).getOpposite())).getBlock() instanceof CopperFitting fitting) {
                    if (ink == ParticleTypes.SQUID_INK) { ink = fitting.ink; }
                    for (int o=0; o<30; o++) {
                        world.spawnParticles(ink, d + ran1.get(world.random) * 0.1, e + ran2.get(world.random) * 0.1, f + ran3.get(world.random) * 0.1, 0, velX, velY, velZ, 0.10000000149011612D);
                    }
                }
            }
        }
        if (itemStack.isOf(Items.SCULK_SENSOR)) {
            double vibX=position.getX();
            double vibY=position.getY();
            double vibZ=position.getZ();
            random1 = (random.nextDouble()*6) - 3;
            random2 = (random.nextDouble()*6) - 3;
            vibX = axis == Direction.Axis.X ? vibX+(10 * offX) : corroded ? (axis==Direction.Axis.Z ? vibX+random2 : vibX+random1) : vibX;
            vibY = axis == Direction.Axis.Y ? vibY+(10 * offY) : corroded ? vibY+random1 : vibY;
            vibZ = axis == Direction.Axis.Z ? vibZ+(10 * offZ) * 2 : corroded ? vibZ+random2 : vibZ;
            BlockPositionSource blockSource = new BlockPositionSource(new BlockPos(vibX, vibY, vibZ));
            world.spawnParticles(new VibrationParticleEffect(blockSource, 32), position.getX(), position.getY(), position.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public int chooseNonEmptySlot(Random random) {
        this.checkLootInteraction(null);
        int i = -1;
        int j = 1;
        for(int k = 0; k < this.inventory.size(); ++k) {
            if (!this.inventory.get(k).isEmpty() && random.nextInt(j++) == 0) {
                i = k;
            }
        } return i;
    }

    private static IntStream getAvailableSlots(Inventory inventory, Direction direction) {
        return inventory instanceof SidedInventory ? IntStream.of(((SidedInventory)inventory).getAvailableSlots(direction)) : IntStream.range(0, inventory.size());
    }

    private static boolean isInventoryFull(Inventory inventory, Direction direction) {
        return getAvailableSlots(inventory, direction).allMatch((i) -> {
            ItemStack itemStack = inventory.getStack(i);
            return itemStack.getCount() >= itemStack.getMaxCount();
        });
    }

    public static ItemStack transfer(Inventory inventory2, ItemStack itemStack, @Nullable Direction direction) {
        if (inventory2 instanceof SidedInventory sidedInventory && direction != null) {
            int[] is = sidedInventory.getAvailableSlots(direction);
            for(int i = 0; i < is.length && !itemStack.isEmpty(); ++i) {
                itemStack = transfer(inventory2, itemStack, is[i], direction);
            }
        } else {
            int sidedInventory = inventory2.size();
            for(int is = 0; is < sidedInventory && !itemStack.isEmpty(); ++is) {
                itemStack = transfer(inventory2, itemStack, is, direction);
            }
        } return itemStack;
    }

    private static boolean canInsert(Inventory inventory, ItemStack itemStack, int i, @Nullable Direction direction) {
        return inventory.isValid(i, itemStack);
    }

    private static ItemStack transfer(Inventory inventory2, ItemStack itemStack, int i, @Nullable Direction direction) {
        ItemStack itemStack2 = inventory2.getStack(i);
        if (canInsert(inventory2, itemStack, i, direction)) {
            boolean bl = false;
            if (itemStack2.isEmpty()) {
                inventory2.setStack(i, itemStack);
                itemStack = ItemStack.EMPTY;
                bl = true;
            } else if (canMergeItems(itemStack2, itemStack)) {
                int j = itemStack.getMaxCount() - itemStack2.getCount();
                int k = Math.min(itemStack.getCount(), j);
                itemStack.decrement(k);
                itemStack2.increment(k);
                bl = k > 0;
            }
            if (bl) {
                inventory2.markDirty();
            }
        } return itemStack;
    }

    @Nullable
    private static Inventory getOutputInventory(World world, BlockPos blockPos, BlockState blockState) {
        return getInventoryAt(world, blockPos.offset(blockState.get(Properties.FACING)));
    }

    @Nullable
    private static Inventory getSecretInventory(World world, BlockPos blockPos, BlockState blockState) {
        return getInventoryAt(world, blockPos.offset(blockState.get(Properties.FACING).getOpposite()));
    }

    private boolean isFull() {
        Iterator<ItemStack> var1 = this.inventory.iterator();
        ItemStack itemStack;
        do {
            if (!var1.hasNext()) {
                return true;
            }
            itemStack = var1.next();
        } while(!itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxCount());
        return false;
    }

    @Nullable
    public static Inventory getInventoryAt(World world, BlockPos blockPos) {
        return getInventoryAt(world, (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D);
    }

    @Nullable
    private static Inventory getInventoryAt(World world, double d, double e, double f) {
        Inventory inventory = null;
        BlockPos blockPos = new BlockPos(d, e, f);
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof InventoryProvider) {
            inventory = ((InventoryProvider)block).getInventory(blockState, world, blockPos);
        } else if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof Inventory) {
                inventory = (Inventory)blockEntity;
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getInventory((ChestBlock)block, blockState, world, blockPos, true);
                }
            }
        }
        if (inventory == null) {
            List<Entity> blockEntity = world.getOtherEntities(null, new Box(d - 0.5D, e - 0.5D, f - 0.5D, d + 0.5D, e + 0.5D, f + 0.5D), EntityPredicates.VALID_INVENTORIES);
            if (!blockEntity.isEmpty()) {
                inventory = (Inventory)blockEntity.get(world.random.nextInt(blockEntity.size()));
            }
        }

        return inventory;
    }

    private static boolean canMergeItems(ItemStack itemStack, ItemStack itemStack2) {
        if (!itemStack.isOf(itemStack2.getItem())) {
            return false;
        } else if (itemStack.getDamage() != itemStack2.getDamage()) {
            return false;
        } else if (itemStack.getCount() > itemStack.getMaxCount()) {
            return false;
        } else {
            return ItemStack.areNbtEqual(itemStack, itemStack2);
        }
    }

    public void setCooldown(BlockState state) {
        int i=2;
        if (state.getBlock() instanceof CopperPipe pipe) {i=pipe.cooldown;}
        this.transferCooldown=i;
    }

    public static void setCooldown(World world, BlockPos blockPos) {
        BlockEntity entity = world.getBlockEntity(blockPos);
        BlockState state = world.getBlockState(blockPos);
        if (state.getBlock() instanceof CopperPipe && entity instanceof CopperPipeEntity pipe) {
            pipe.setCooldown(state);
        }
    }

    public int canWater(World world, BlockPos blockPos, BlockState blockState) {
        if (blockState.get(Properties.WATERLOGGED)) {return 12;}
        BlockPos p = blockPos.offset(blockState.get(Properties.FACING).getOpposite());
        if (!world.isChunkLoaded(p)) { return 0; }
        return CopperPipe.waterLevel(world, p);
    }

    public int canSmoke(World world, BlockPos blockPos, BlockState blockState) {
        BlockPos p = blockPos.offset(blockState.get(Properties.FACING).getOpposite());
        if (!world.isChunkLoaded(p)) { return 0; }
        return CopperPipe.smokeLevel(world, p);
    }

    public int getDecreasedSmoke() {
        return Math.max(this.smokeLevel - 1, 0);
    }

    public int getDecreasedWater() {
        return Math.max(this.waterLevel - 1, 0);
    }

    private static boolean isPipe(World world, BlockPos blockPos, Direction direction) {
        BlockState state = world.getBlockState(blockPos.offset(direction.getOpposite()));
        if (state.getBlock() instanceof CopperPipe) {return state.get(Properties.FACING) == direction.getOpposite();}
        return false;
    }

    public GameEventListener getGameEventListener() {
        return this.listener;
    }

    public void readNbt(NbtCompound nbtCompound) {
        super.readNbt(nbtCompound);
        this.transferCooldown = nbtCompound.getInt("transferCooldown");
        this.dispenseCooldown = nbtCompound.getInt("dispenseCooldown");
        this.noteBlockCooldown = nbtCompound.getInt("noteBlockCooldown");
        if (nbtCompound.contains("listener", 10)) {
            DataResult<?> var10000 = CopperPipeListener.createPipeCodec(this).parse(new Dynamic<>(NbtOps.INSTANCE, nbtCompound.getCompound("listener")));
            Logger var10001 = LOGGER;
            Objects.requireNonNull(var10001);
            var10000.resultOrPartial(var10001::error).ifPresent((vibrationListener) -> this.listener = (CopperPipeListener) vibrationListener);
        }
        this.extraPipeData = ExtraPipeData.readNbt(nbtCompound);
    }

    protected void writeNbt(NbtCompound nbtCompound) {
        super.writeNbt(nbtCompound);
        nbtCompound.putInt("transferCooldown", this.transferCooldown);
        nbtCompound.putInt("dispenseCooldown", this.dispenseCooldown);
        nbtCompound.putInt("noteBlockCooldown", this.noteBlockCooldown);
        DataResult<?> var10000 = CopperPipeListener.createPipeCodec(this).encodeStart(NbtOps.INSTANCE, this.listener);
        Logger var10001 = LOGGER;
        Objects.requireNonNull(var10001);
        var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> nbtCompound.put("listener", (NbtElement)nbtElement));
        ExtraPipeData.writeNbt(nbtCompound, this.extraPipeData);
    }

    public static boolean notCubeNorPipe(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return !(block instanceof CopperPipe) && !(block instanceof CopperFitting) && !state.isSolidBlock(world, pos);
    }

    @Override
    public boolean accepts(ServerWorld serverWorld, GameEventListener gameEventListener, BlockPos blockPos, GameEvent gameEvent, GameEvent.Emitter emitter) {
        BlockState thisState = serverWorld.getBlockState(this.getPos());
        if (serverWorld.getBlockState(blockPos).getBlock() instanceof CopperPipe) { return false; }
        if (thisState.getBlock() instanceof CopperPipe) {
            boolean bl = gameEvent == GameEvent.BLOCK_DESTROY && blockPos.equals(this.getPos());
            boolean bl2 = gameEvent == GameEvent.BLOCK_PLACE && blockPos.equals(this.getPos());
            boolean bl3 = notCubeNorPipe(serverWorld, this.getPos().offset(thisState.get(Properties.FACING).getOpposite()));
            if (!bl && !bl2 && bl3) {
                this.moveablePipeDataHandler.addSaveableMoveablePipeNbt(new MoveablePipeDataHandler.SaveableMovablePipeNbt(gameEvent, Vec3d.ofCenter(blockPos), emitter, this.getPos()).withShouldMove(true).withShouldSave(true));
                return true;
            }
        } return false;
    }

    @Override
    public void accept(ServerWorld serverWorld, GameEventListener gameEventListener, BlockPos blockPos, GameEvent gameEvent, @Nullable Entity entity, @Nullable Entity entity2, float f) {

    }

    @Override
    public boolean canAccept(GameEvent gameEvent, GameEvent.Emitter emitter) {
        Entity entity = emitter.comp_713();
        if (entity != null) {
            if (entity.isSpectator()) {
                return false;
            }

            if (entity.bypassesSteppingEffects() && gameEvent.isIn(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
                return false;
            }

            if (entity.occludeVibrationSignals()) {
                return false;
            }
        }

        if (emitter.comp_714() != null) {
            return !emitter.comp_714().isIn(BlockTags.DAMPENS_VIBRATIONS);
        } else {
            return true;
        }
    }

    public void onListen() {
        this.markDirty();
    }

    public boolean canAcceptMoveableNbt(MoveablePipeDataHandler.MOVE_TYPE moveType, Direction moveDirection, BlockState fromState) {
        if (moveType == MoveablePipeDataHandler.MOVE_TYPE.FROM_FITTING) {
            return this.getCachedState().get(Properties.FACING) == moveDirection;
        }
        return this.getCachedState().get(Properties.FACING) == moveDirection || moveDirection == fromState.get(Properties.FACING);
    }

    public boolean canMoveNbtInDirection(Direction direction, BlockState state) {
        return direction != state.get(Properties.FACING).getOpposite();
    }

    public void dispenseMoveableNbt(ServerWorld serverWorld, BlockPos blockPos, BlockState blockState) {
        Direction direction = blockState.get(Properties.FACING);
        Direction directionOpp = direction.getOpposite();
        Block dirBlock = serverWorld.getBlockState(blockPos.offset(direction)).getBlock();
        Block oppBlock = serverWorld.getBlockState(blockPos.offset(directionOpp)).getBlock();
        boolean bl1 = dirBlock == Blocks.AIR;
        boolean bl2 = oppBlock != Blocks.AIR;
        boolean bl3 = dirBlock == Blocks.WATER;
        boolean bl4 = oppBlock != Blocks.WATER;
        if ((bl1 || bl3) && (bl2 && bl4)) {
            ArrayList<MoveablePipeDataHandler.SaveableMovablePipeNbt> nbtList = this.moveablePipeDataHandler.getSavedNbtList();
            if (!nbtList.isEmpty()) {
                LOGGER.error("dispense");
                for (MoveablePipeDataHandler.SaveableMovablePipeNbt nbt : nbtList) {
                    if (nbt.getShouldMove()) {
                        nbt.dispense(serverWorld, blockPos, blockState, this);
                    }
                }
                this.moveMoveableNbt(serverWorld, blockPos, blockState);
            }
        }
    }

    public boolean listenersNearby(World world, BlockPos pos) {
        if (this.extraPipeData!=null) {
            if (world.getBlockState(this.extraPipeData.listenerPos).isIn(Main.BLOCK_LISTENERS)) { return true; }
        }
        int bx = pos.getX();
        int by = pos.getY();
        int bz = pos.getZ();
        for(int x = bx - 8; x <= bx + 8; x++) {
            for(int y = by - 8; y <= by + 8; y++) {
                for(int z = bz - 8; z <= bz + 8; z++) {
                    double distance = ((bx-x) * (bx-x) + ((bz-z) * (bz-z)) + ((by-y) * (by-y)));
                    if(distance < 81) {
                        BlockPos l = new BlockPos(x, y, z);
                        if (world.getBlockState(l).isIn(Main.BLOCK_LISTENERS)) {
                            this.extraPipeData = new ExtraPipeData(l);
                            return true;
                        }
                    }
                }
            }
        }
        this.extraPipeData = null;
        List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, new Box(pos.add(-18, -18, -18), pos.add(18, 18, 18)));
        for (Entity entity : entities) {
            if (entity.getType().isIn(Main.ENTITY_LISTENERS) && Math.floor(Math.sqrt(entity.getBlockPos().getSquaredDistance(pos))) <= 16) {
                return true;
            }
        }
        return false;
    }

}
