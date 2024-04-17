package net.jayugg.leanclass.block;

import net.jayugg.leanclass.base.AbilityType;
import net.jayugg.leanclass.item.ModItems;
import net.jayugg.leanclass.util.PlayerClassManager;
import net.jayugg.leanclass.base.SkillSlot;
import net.minecraft.block.*;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkillAltarBlock extends BlockWithEntity {
    public static EnumProperty<AltarCharge> ALTAR_CHARGE = EnumProperty.of("altar_charge", AltarCharge.class);
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
    public static final List<BlockPos> HOLDER_OFFSETS = BlockPos.stream(-2, 0, -2, 2, 0, 2)
            .filter((pos) -> (Math.abs(pos.getX()) == 2 && pos.getZ() == 0) || (Math.abs(pos.getZ()) == 2 && pos.getX() == 0) || (pos.getZ() == -2 && pos.getX() == 0))
            .map(BlockPos::toImmutable).toList();

    private static final Map<BlockPos, SkillSlot> OFFSET_TO_SKILL_SLOT = Map.of(
            new BlockPos(-2, 0, 0), SkillSlot.WEST,
            new BlockPos(2, 0, 0), SkillSlot.EAST,
            new BlockPos(0, 0, -2), SkillSlot.NORTH,
            new BlockPos(0, 0, 2), SkillSlot.SOUTH
    );

    public SkillAltarBlock(Settings settings) {
        super(settings.luminance(state -> state.get(ALTAR_CHARGE) == AltarCharge.INERT ? 5 : 12));
        this.setDefaultState(this.stateManager.getDefaultState().with(ALTAR_CHARGE, AltarCharge.INERT));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ALTAR_CHARGE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // Handle charging

    private static boolean isChargeItem(ItemStack stack) {
        return stack.isOf(Items.MAGMA_BLOCK) || stack.isOf(Items.AMETHYST_BLOCK);
    }

    public static void loadChargeItem(@Nullable Entity charger, World world, BlockPos pos, BlockState state, ItemStack stack) {
        AltarCharge charge = stack.isOf(Items.MAGMA_BLOCK) ? AltarCharge.ACTIVE : AltarCharge.PASSIVE;
        BlockState blockState = state.with(ALTAR_CHARGE, charge);
        DefaultParticleType particleType = charge == AltarCharge.ACTIVE ? ParticleTypes.LANDING_LAVA : ParticleTypes.LANDING_OBSIDIAN_TEAR;
        world.addParticle(particleType, (double)pos.getX() + 0.5, (double)pos.getY() + 1.0, (double)pos.getZ() + 0.5, 0.0, 0.0, 0.0);
        updateChargeItem(charger, world, pos, blockState);
    }

    public static void unloadChargeItem(@Nullable Entity charger, World world, BlockPos pos, BlockState state) {
        BlockState blockState = state.with(ALTAR_CHARGE, AltarCharge.INERT);
        updateChargeItem(charger, world, pos, blockState);
    }

private static void updateChargeItem(@Nullable Entity charger, World world, BlockPos pos, BlockState blockState) {
        world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(charger, blockState));
        world.playSound(null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.BLOCKS, 1.5f, 0.5f);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getStackInHand(hand);
        boolean isChargeItem = isChargeItem(itemStack);
        boolean isCreativeMode = player.getAbilities().creativeMode;

        if (hand == Hand.MAIN_HAND && !isChargeItem && isChargeItem(player.getStackInHand(Hand.OFF_HAND))) {
            return ActionResult.PASS;
        }

        if (isChargeItem && state.get(ALTAR_CHARGE) == AltarCharge.INERT) {
            loadChargeItem(player, world, pos, state, itemStack);
            if (!isCreativeMode) {
                itemStack.decrement(1);
            }
        } else if (state.get(ALTAR_CHARGE) != AltarCharge.INERT) {
            if (itemStack.isEmpty()) {
                player.setStackInHand(hand, new ItemStack(state.get(ALTAR_CHARGE) == AltarCharge.ACTIVE ? Items.MAGMA_BLOCK : Items.AMETHYST_BLOCK));
                unloadChargeItem(player, world, pos, state);
            } else if (isChargeItem) {
                if (!isCreativeMode) {
                    ItemStack stack = new ItemStack(state.get(ALTAR_CHARGE) == AltarCharge.ACTIVE ? Items.MAGMA_BLOCK : Items.AMETHYST_BLOCK);
                    if (!player.getInventory().insertStack(stack)) {
                        player.dropItem(stack, false);
                    }
                }
                unloadChargeItem(player, world, pos, state);
            } else {
                return ActionResult.PASS;
            }
        } else {
            return ActionResult.PASS;
        }

        return ActionResult.success(world.isClient);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (state.get(ALTAR_CHARGE) != AltarCharge.INERT) {
            world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(state.get(ALTAR_CHARGE) == AltarCharge.ACTIVE ? Items.MAGMA_BLOCK : Items.AMETHYST_BLOCK)));
        }
        super.onBreak(world, pos, state, player);
    }

    // Handle Skill Up
    public static boolean canAccessShard(World world, BlockPos altarPos, BlockPos holderOffset) {
        BlockState blockState = world.getBlockState(altarPos.add(holderOffset));
        if (blockState.isOf(ModBlocks.SHARD_HOLDER)) {
            return blockState.get(ShardHolderBlock.HAS_SHARD) && world.isAir(altarPos.add(holderOffset.getX() / 2, holderOffset.getY(), holderOffset.getZ() / 2));
        }
        return false;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);

        DefaultParticleType particleType = null;
        if (state.get(ALTAR_CHARGE) == AltarCharge.ACTIVE) {
            particleType = ParticleTypes.DRIPPING_LAVA;
        } else if (state.get(ALTAR_CHARGE) == AltarCharge.PASSIVE) {
            particleType = ParticleTypes.DRIPPING_OBSIDIAN_TEAR;
        }

        if (particleType != null && random.nextInt(10) == 0) {
            Direction direction = Direction.random(random);
            if (direction != Direction.UP) {
                BlockPos blockPos = pos.offset(direction);
                BlockState blockState = world.getBlockState(blockPos);
                if (!state.isOpaque() || !blockState.isSideSolidFullSquare(world, blockPos, direction.getOpposite())) {
                    double d = direction.getOffsetX() == 0 ? random.nextDouble() : 0.5 + (double)direction.getOffsetX() * 0.6;
                    double e = (direction.getOffsetY() == 0 ? random.nextDouble() : 0.5 + (double)direction.getOffsetY() * 0.6)* 0.75;
                    double f = direction.getOffsetZ() == 0 ? random.nextDouble() : 0.5 + (double)direction.getOffsetZ() * 0.6;
                    world.addParticle(particleType, (double)pos.getX() + d, (double)pos.getY() + e, (double)pos.getZ() + f, 0.0, 0.0, 0.0);
                }
            }
        }

        for (BlockPos blockPos : HOLDER_OFFSETS) {
            if (random.nextInt(8) == 0 && canAccessShard(world, pos, blockPos)) {
                world.addParticle(ParticleTypes.ENCHANT, (double) pos.getX() + 0.5, (double) pos.getY() + 2, (double) pos.getZ() + 0.5, (double) ((float) blockPos.getX() + random.nextFloat()) - 0.5, (float) blockPos.getY() - random.nextFloat() - 1.0F, (double) ((float) blockPos.getZ() + random.nextFloat()) - 0.5);
            }
        }

    }

    @Override
    public void onEntityLand(BlockView world, Entity entity) {
        if (entity.bypassesLandingEffects() || isShardSlotEmpty(world, entity.getBlockPos())) {
            super.onEntityLand(world, entity);
        } else if (entity instanceof PlayerEntity player) {
            useShard(player, (World) world);
        }
    }

    private static void failBehaviour(World world, PlayerEntity player, BlockPos holderPos){
        player.damage(world.getDamageSources().magic(), 4.0f);
        world.setBlockState(holderPos, world.getBlockState(holderPos).with(ShardHolderBlock.HAS_SHARD, false));
        ItemEntity itemEntity = new ItemEntity(world, holderPos.getX() + 0.5, holderPos.getY() + 0.8, holderPos.getZ() + 0.5, new ItemStack(ModItems.SKILL_SHARD));
        itemEntity.setVelocity(0, 0, 0);
        world.spawnEntity(itemEntity);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.BLOCKS, 1.2f, 0.75f);
    }

    private void successEffects(World world, PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 30, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 30, 1));
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 1.2f, 1.25f);
    }

    private boolean isShardSlotEmpty(BlockView world, BlockPos pos) {
        return getActiveHolders(world, pos).isEmpty();
    }

    private void useShard(PlayerEntity player, World world) {
        if (world.isClient) {
            return;
        }
        List<BlockPos> activeHolders = getActiveHolders(world, player.getBlockPos());
        BlockState altarState = world.getBlockState(player.getBlockPos());
        if (activeHolders.isEmpty() || altarState.get(ALTAR_CHARGE) == AltarCharge.INERT) {
            return;
        }
        BlockPos holderPos = activeHolders.get(0);
        BlockState holderState = world.getBlockState(holderPos);
        if (!holderState.isOf(ModBlocks.SHARD_HOLDER) || !holderState.get(ShardHolderBlock.HAS_SHARD)) {
            return;
        }
        SkillSlot skillSlot = offsetToSkillSlot(holderPos.subtract(player.getBlockPos()));
        AbilityType type = altarState.get(ALTAR_CHARGE) == AltarCharge.ACTIVE ? AbilityType.ACTIVE : AbilityType.PASSIVE;
        if (skillSlot != null && PlayerClassManager.skillUp(player, type, skillSlot)) {
            world.setBlockState(holderPos, holderState.with(ShardHolderBlock.HAS_SHARD, false));
            world.setBlockState(player.getBlockPos(), altarState.with(ALTAR_CHARGE, AltarCharge.INERT));
            bounce(player);
            successEffects(world, player);
        } else {
            failBehaviour(world, player, holderPos);
            bounce(player);
        }
    }

    private SkillSlot offsetToSkillSlot(BlockPos offset) {
        return OFFSET_TO_SKILL_SLOT.get(offset);
    }

    //get Holders

    private List<BlockPos> getActiveHolders(BlockView world, BlockPos pos) {
        List<BlockPos> activeHolders = new ArrayList<>();
        BlockPos.Mutable holderPos = new BlockPos.Mutable();
        for (BlockPos offset : HOLDER_OFFSETS) {
            holderPos.set(pos).move(offset);
            BlockState holderState = world.getBlockState(holderPos);
            if (holderState.isOf(ModBlocks.SHARD_HOLDER) && holderState.get(ShardHolderBlock.HAS_SHARD)) {
                activeHolders.add(holderPos.toImmutable());
            }
        }
        return activeHolders;
    }

    private void bounce(Entity entity) {
        Vec3d vec3d = entity.getVelocity();
        if (vec3d.y < 0.0 && entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).jump();
        }
    }

    // Block entity

    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SkillAltarBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? checkType(type, ModBlockEntities.SKILL_ALTAR, SkillAltarBlockEntity::tick) : null;
    }
}
