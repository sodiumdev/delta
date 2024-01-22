package zip.sodium.delta.player;

import com.mojang.logging.LogUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.slf4j.Logger;
import zip.sodium.delta.helper.BlockHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DeltaSPGM extends ServerPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServerPlayer player;
    public ServerLevel level; // Paper - Anti-Xray - protected -> public
    public boolean captureSentBlockEntities = false; // Paper - Send block entities after destroy prediction
    public boolean capturedBlockEntity = false; // Paper - Send block entities after destroy prediction
    // CraftBukkit start - whole method
    public boolean interactResult = false;
    public boolean firedInteract = false;
    public BlockPos interactPosition;
    public InteractionHand interactHand;
    public ItemStack interactItemStack;
    private GameType gameModeForPlayer;
    @Nullable
    private GameType previousGameModeForPlayer;
    private boolean isDestroyingBlock;
    private int destroyProgressStart;
    private BlockPos destroyPos;
    private int gameTicks;
    private boolean hasDelayedDestroy;
    private BlockPos delayedDestroyPos;
    private int delayedTickStart;
    private int lastSentState;

    private int sequence = 0;
    private float currentBreakingProgress;
    private int blockBreakingCooldown;
    private boolean hasMiningFatigue;

    public DeltaSPGM(ServerPlayer player, ServerPlayerGameMode spgm) {
        super(player);

        this.gameModeForPlayer = spgm.getGameModeForPlayer();
        this.previousGameModeForPlayer = spgm.getPreviousGameModeForPlayer();
        this.interactHand = spgm.interactHand;

        this.destroyPos = BlockPos.ZERO;
        this.delayedDestroyPos = BlockPos.ZERO;
        this.lastSentState = -1;
        this.player = player;
        this.level = player.serverLevel();
    }

    public boolean changeGameModeForPlayer(GameType gameMode) {
        // Paper end - Expand PlayerGameModeChangeEvent
        PlayerGameModeChangeEvent event = this.changeGameModeForPlayer(gameMode, PlayerGameModeChangeEvent.Cause.UNKNOWN, null);
        return event != null && event.isCancelled();
    }

    @Nullable
    public PlayerGameModeChangeEvent changeGameModeForPlayer(GameType gameMode, PlayerGameModeChangeEvent.Cause cause, @Nullable Component cancelMessage) {
        // Paper end - Expand PlayerGameModeChangeEvent
        if (gameMode == this.gameModeForPlayer) {
            return null; // Paper - Expand PlayerGameModeChangeEvent
        }

        // CraftBukkit start
        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(this.player.getBukkitEntity(), GameMode.getByValue(gameMode.getId()), cause, cancelMessage); // Paper
        this.level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return event; // Paper - Expand PlayerGameModeChangeEvent
        }
        // CraftBukkit end
        this.setGameModeForPlayer(gameMode, this.gameModeForPlayer); // Paper - Fix MC-259571
        this.player.onUpdateAbilities();
        this.player.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, this.player), this.player); // CraftBukkit
        this.level.updateSleepingPlayerList();
        return event; // Paper - Expand PlayerGameModeChangeEvent
    }

    protected void setGameModeForPlayer(GameType gameMode, @Nullable GameType previousGameMode) {
        this.previousGameModeForPlayer = previousGameMode;
        this.gameModeForPlayer = gameMode;
        gameMode.updatePlayerAbilities(this.player.getAbilities());
    }

    public GameType getGameModeForPlayer() {
        return this.gameModeForPlayer;
    }

    @Nullable
    public GameType getPreviousGameModeForPlayer() {
        return this.previousGameModeForPlayer;
    }

    public boolean isSurvival() {
        return this.gameModeForPlayer.isSurvival();
    }

    public boolean isCreative() {
        return this.gameModeForPlayer.isCreative();
    }

    public void tick() {
        this.gameTicks = (int) this.level.getLagCompensationTick(); // CraftBukkit; // Paper - lag compensation
        BlockState iblockdata;

        if (this.hasDelayedDestroy) {
            iblockdata = this.level.getBlockStateIfLoaded(this.delayedDestroyPos); // Paper
            if (iblockdata == null || iblockdata.isAir()) { // Paper
                this.hasDelayedDestroy = false;
            } else {
                float f = this.incrementDestroyProgress(iblockdata, this.delayedDestroyPos, this.delayedTickStart);

                if (f >= 1.0F) {
                    this.hasDelayedDestroy = false;
                    this.destroyBlock(this.delayedDestroyPos);
                }
            }
        } else if (this.isDestroyingBlock) {
            // Paper start - don't want to do same logic as above, return instead
            iblockdata = this.level.getBlockStateIfLoaded(this.destroyPos);
            if (iblockdata == null) {
                this.isDestroyingBlock = false;
                return;
            }
            // Paper end
            if (iblockdata.isAir()) {
                this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                this.lastSentState = -1;
                this.isDestroyingBlock = false;
            } else {
                this.incrementDestroyProgress(iblockdata, this.destroyPos, this.destroyProgressStart);
            }
        }

    }

    private float incrementDestroyProgress(BlockState state, BlockPos pos, int failedStartMiningTime) {
        int j = this.gameTicks - failedStartMiningTime;
        float f = state.getDestroyProgress(this.player, this.player.level(), pos) *  (j + 1);
        int k = (int) (f * 10.0F);

        if (k != this.lastSentState) {
            this.level.destroyBlockProgress(this.player.getId(), pos, k);
            this.lastSentState = k;
        }

        breakIfTakingTooLong(state, pos);

        return f;
    }

    private void breakIfTakingTooLong(BlockState state, BlockPos pos) {
        if (!BlockHelper.shouldMineServerSide(player, state, pos)) {
            if (hasMiningFatigue)
                clearMiningEffect();

            return;
        }

        if (blockBreakingCooldown > 0) {
            blockBreakingCooldown--;

            return;
        }

        currentBreakingProgress += state.getDestroyProgress(player, player.level(), pos);

        if (currentBreakingProgress >= 1) {
            blockBreakingCooldown = 5;
            currentBreakingProgress = 0;

            player.connection.send(new ClientboundBlockDestructionPacket(-1, pos, -1));
            destroyAndAck(pos, sequence, "destroyed");

            return;
        }

        final int progress = currentBreakingProgress > 0
                ? (int) (currentBreakingProgress * 10)
                : -1;

        player.connection.send(new ClientboundBlockDestructionPacket(-1, pos, progress));
    }

    private void debugLogging(BlockPos pos, boolean success, int sequence, String reason) {
    }

    public void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight, int sequence) {
        packetReceived(pos, action, sequence);

        if (this.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE) {
            return;
        } else if (pos.getY() >= worldHeight) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
            this.debugLogging(pos, false, sequence, "too high");
        }

        BlockState blockState;

        switch (action) {
            case START_DESTROY_BLOCK -> {
                if (!this.level.mayInteract(this.player, pos)) {
                    // CraftBukkit start - fire PlayerInteractEvent
                    CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, pos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
                    this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
                    this.debugLogging(pos, false, sequence, "may not interact");
                    // Update any tile entity data for this block
                    capturedBlockEntity = true; // Paper - Send block entities after destroy prediction
                    // CraftBukkit end
                    return;
                }

                // CraftBukkit start
                PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, pos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
                if (event.isCancelled()) {
                    capturedBlockEntity = true; // Paper - Send block entities after destroy prediction
                    return;
                }
                // CraftBukkit end

                if (this.isCreative()) {
                    this.destroyAndAck(pos, sequence, "creative destroy");
                    return;
                }

                // Spigot start - handle debug stick left click for non-creative
                if (this.player.getMainHandItem().is(Items.DEBUG_STICK)
                        && ((DebugStickItem) Items.DEBUG_STICK).handleInteraction(this.player, this.level.getBlockState(pos), this.level, pos, false, this.player.getMainHandItem())) {
                    // this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos)); // Paper - Don't resync block
                    return;
                }
                // Spigot end

                if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
                    this.debugLogging(pos, false, sequence, "block action restricted");
                    return;
                }

                this.destroyProgressStart = this.gameTicks;
                float f = 1.0F;

                blockState = this.level.getBlockState(pos);
                // CraftBukkit start - Swings at air do *NOT* exist.
                if (event.useInteractedBlock() != Event.Result.DENY && !blockState.isAir()) {
                    blockState.attack(this.level, pos, this.player);
                    f = blockState.getDestroyProgress(this.player, this.player.level(), pos);
                }

                if (event.useItemInHand() == Event.Result.DENY)
                    return;

                BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.player, pos, direction, this.player.getInventory().getSelected(), f >= 1.0f); // Paper - Add BlockFace to BlockDamageEvent

                if (blockEvent.isCancelled()) {
                    // Let the client know the block still exists
                    // this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos)); // Paper - Don't resync block
                    return;
                }

                if (blockEvent.getInstaBreak()) {
                    f = 2.0f;
                }
                // CraftBukkit end

                if (!blockState.isAir() && f >= 1.0F) {
                    this.destroyAndAck(pos, sequence, "insta mine");
                } else {
                    if (this.isDestroyingBlock) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(this.destroyPos, this.level.getBlockState(this.destroyPos)));
                        this.debugLogging(pos, false, sequence, "abort destroying since another started (client insta mine, server disagreed)");
                    }

                    this.isDestroyingBlock = true;
                    this.destroyPos = pos.immutable();
                    int k = (int) (f * 10.0F);

                    currentBreakingProgress = 0;
                    this.level.destroyBlockProgress(this.player.getId(), pos, k);
                    this.debugLogging(pos, true, sequence, "actual start of destroying");
                    this.lastSentState = k;
                }
            }

            case STOP_DESTROY_BLOCK -> {
                if (pos.equals(this.destroyPos)) {
                    int l = this.gameTicks - this.destroyProgressStart;

                    blockState = this.level.getBlockState(pos);
                    if (!blockState.isAir()) {
                        float f1 = blockState.getDestroyProgress(this.player, this.player.level(), pos) * (l + 1);

                        if (f1 >= 0.7F) {
                            this.isDestroyingBlock = false;
                            this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                            this.destroyAndAck(pos, sequence, "destroyed");
                            return;
                        }

                        if (!this.hasDelayedDestroy) {
                            this.isDestroyingBlock = false;
                            this.hasDelayedDestroy = true;
                            this.delayedDestroyPos = pos;
                            this.delayedTickStart = this.destroyProgressStart;
                        }
                    }
                }

                this.debugLogging(pos, true, sequence, "stopped destroying");
            }

            case ABORT_DESTROY_BLOCK -> {
                this.isDestroyingBlock = false;
                if (!Objects.equals(this.destroyPos, pos) && !BlockPos.ZERO.equals(this.destroyPos)) { // Paper
                    LOGGER.debug("Mismatch in destroy block pos: {} {}", this.destroyPos, pos); // CraftBukkit - SPIGOT-5457 sent by client when interact event cancelled
                    BlockState type = this.level.getBlockStateIfLoaded(this.destroyPos); // Paper - don't load unloaded chunks for stale records here
                    if (type != null) this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                    if (type != null) this.debugLogging(pos, true, sequence, "aborted mismatched destroying");
                    this.destroyPos = BlockPos.ZERO; // Paper
                }

                this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                this.debugLogging(pos, true, sequence, "aborted destroying");

                CraftEventFactory.callBlockDamageAbortEvent(this.player, pos, this.player.getInventory().getSelected()); // CraftBukkit
            }

            default -> {}
        }

        this.level.chunkPacketBlockController.onPlayerLeftClickBlock(this, pos, action, direction, worldHeight, sequence); // Paper - Anti-Xray

        enforceBlockBreakingCooldown(pos, action);
    }

    private void enforceBlockBreakingCooldown(BlockPos pos, ServerboundPlayerActionPacket.Action action) {
        final var state = level.getBlockState(pos);
        if (!BlockHelper.shouldMineServerSide(player, state, pos)) {
            if (hasMiningFatigue)
                clearMiningEffect();

            return;
        }

        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)
            destroyProgressStart += blockBreakingCooldown;
    }

    private void sendMiningFatigue() {
        hasMiningFatigue = true;
        player.connection.send(new ClientboundUpdateMobEffectPacket(
                player.getId(),
                new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN,
                        20,
                        -1,
                        true,
                        false
                )
        ));
    }

    private void clearMiningEffect() {
        hasMiningFatigue = false;
        player.connection.send(new ClientboundRemoveMobEffectPacket(
                player.getId(),
                MobEffects.DIG_SLOWDOWN
        ));

        if (player.hasEffect(MobEffects.DIG_SLOWDOWN))
            player.connection.send(new ClientboundUpdateMobEffectPacket(
                    player.getId(),
                    player.getEffect(MobEffects.DIG_SLOWDOWN)
            ));
    }

    private void packetReceived(BlockPos pos, ServerboundPlayerActionPacket.Action action, int sequence) {
        this.sequence = sequence;

        var state = player.level().getBlockState(pos);
        if (!BlockHelper.shouldMineServerSide(player, state, pos)) {
            if (hasMiningFatigue)
                clearMiningEffect();

            return;
        }

        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            this.currentBreakingProgress = 0;
            var ogDelta = state.getDestroyProgress(player, level, pos);

            state = BlockHelper.getReplacementBlockState(state, player);
            float delta = state.getDestroyProgress(player, level, pos);

            if (delta >= 1.0f && ogDelta < 1.0f)
                this.player.connection.send(new ClientboundBlockUpdatePacket(pos, state));

            if (ogDelta < 1.0f)
                sendMiningFatigue();
        } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            if (this.hasMiningFatigue)
                this.clearMiningEffect();

            player.connection.send(new ClientboundBlockDestructionPacket(-1, pos, -1));
        }
    }

    public void destroyAndAck(BlockPos pos, int sequence, String reason) {
        clearMiningEffect();

        if (this.destroyBlock(pos)) {
            this.debugLogging(pos, true, sequence, reason);
        } else {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
            this.debugLogging(pos, false, sequence, reason);
        }

    }

    public boolean destroyBlock(BlockPos pos) {
        BlockState blockState = this.level.getBlockState(pos);
        // CraftBukkit start - fire BlockBreakEvent
        org.bukkit.block.Block bblock = CraftBlock.at(this.level, pos);
        BlockBreakEvent event = null;

        if (this.player != null) {
            boolean isSwordNoBreak = !this.player.getMainHandItem().getItem().canAttackBlock(blockState, this.level, pos, this.player);

            event = new BlockBreakEvent(bblock, this.player.getBukkitEntity());
            event.setCancelled(isSwordNoBreak);

            BlockState nmsData = this.level.getBlockState(pos);
            Block nmsBlock = nmsData.getBlock();

            ItemStack itemstack = this.player.getItemBySlot(EquipmentSlot.MAINHAND);

            if (!event.isCancelled()
                    && !this.isCreative()
                    && this.player.hasCorrectToolForDrops(nmsBlock.defaultBlockState()))
                event.setExpToDrop(nmsBlock.getExpDrop(nmsData, this.level, pos, itemstack, true));

            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                if (isSwordNoBreak)
                    return false;

                // Update any tile entity data for this block
                if (!captureSentBlockEntities) { // Paper - Send block entities after destroy prediction
                    BlockEntity tileentity = this.level.getBlockEntity(pos);
                    if (tileentity != null) {
                        this.player.connection.send(tileentity.getUpdatePacket());
                    }
                } else capturedBlockEntity = true;
                // Paper - Send block entities after destroy prediction

                return false;
            }
        }
        // CraftBukkit end

        blockState = this.level.getBlockState(pos); // CraftBukkit - update state from plugins
        if (blockState.isAir())
            return false; // CraftBukkit - A plugin set block to air without cancelling

        BlockEntity tileentity = this.level.getBlockEntity(pos);
        Block block = blockState.getBlock();

        if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks() && !(block instanceof CommandBlock && (this.player.isCreative() && this.player.getBukkitEntity().hasPermission("minecraft.commandblock")))) { // Paper - command block permission
            this.level.sendBlockUpdated(pos, blockState, blockState, 3);
            return false;
        } else if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
            return false;
        }

        // CraftBukkit start
        this.level.captureDrops = new ArrayList<>();
        // CraftBukkit end

        BlockState iblockdata1 = block.playerWillDestroy(this.level, pos, blockState, this.player);
        boolean flag = this.level.removeBlock(pos, false);

        if (flag)
            block.destroy(this.level, pos, iblockdata1);

        ItemStack mainHandStack = null; // Paper - Trigger bee_nest_destroyed trigger in the correct place
        boolean isCorrectTool = false; // Paper - Trigger bee_nest_destroyed trigger in the correct place
        if (!this.isCreative()) {
            ItemStack itemstack = this.player.getMainHandItem();
            ItemStack itemstack1 = itemstack.copy();
            boolean flag1 = this.player.hasCorrectToolForDrops(iblockdata1);
            mainHandStack = itemstack1; // Paper - Trigger bee_nest_destroyed trigger in the correct place
            isCorrectTool = flag1; // Paper - Trigger bee_nest_destroyed trigger in the correct place

            itemstack.mineBlock(this.level, iblockdata1, pos, this.player);
            if (flag && flag1/* && event.isDropItems() */) { // CraftBukkit - Check if block should drop items // Paper - fix drops not preventing stats/food exhaustion
                block.playerDestroy(this.level, this.player, pos, iblockdata1, tileentity, itemstack1, event.isDropItems(), false); // Paper - fix drops not preventing stats/food exhaustion
            }

            // return true; // CraftBukkit
        }
        // CraftBukkit start
        List<ItemEntity> itemsToDrop = this.level.captureDrops; // Paper - capture all item additions to the world
        this.level.captureDrops = null; // Paper - capture all item additions to the world; Remove this earlier so that we can actually drop stuff
        if (event.isDropItems())
            CraftEventFactory.handleBlockDropItemEvent(bblock, bblock.getState(), this.player, itemsToDrop); // Paper - capture all item additions to the world

        //this.level.captureDrops = null; // Paper - capture all item additions to the world; move up

        // Drop event experience
        if (flag)
            blockState.getBlock().popExperience(this.level, pos, event.getExpToDrop(), this.player); // Paper

        // Paper start - Trigger bee_nest_destroyed trigger in the correct place (check impls of block#playerDestroy)
        if (mainHandStack != null
                && flag
                && isCorrectTool
                && event.isDropItems()
                && block instanceof BeehiveBlock
                && tileentity instanceof BeehiveBlockEntity beehiveBlockEntity) // simulates the guard on block#playerDestroy above
            CriteriaTriggers.BEE_NEST_DESTROYED.trigger(player, blockState, mainHandStack, beehiveBlockEntity.getOccupantCount());
        // Paper end - Trigger bee_nest_destroyed trigger in the correct place

        return true;
        // CraftBukkit end
    }

    public InteractionResult useItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand) {
        if (this.gameModeForPlayer == GameType.SPECTATOR
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        }

        int i = stack.getCount();
        int j = stack.getDamageValue();
        InteractionResultHolder<ItemStack> interactionresultwrapper = stack.use(world, player, hand);
        ItemStack itemstack1 = interactionresultwrapper.getObject();

        if ((itemstack1 == stack && itemstack1.getCount() == i && itemstack1.getUseDuration() <= 0 && itemstack1.getDamageValue() == j)
                || (interactionresultwrapper.getResult() == InteractionResult.FAIL && itemstack1.getUseDuration() > 0 && !player.isUsingItem()))
            return interactionresultwrapper.getResult();

        if (stack != itemstack1) {
            player.setItemInHand(hand, itemstack1);
        }

        if (this.isCreative() && itemstack1 != ItemStack.EMPTY) {
            itemstack1.setCount(i);
            if (itemstack1.isDamageableItem() && itemstack1.getDamageValue() != j) {
                itemstack1.setDamageValue(j);
            }
        }

        if (itemstack1.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }

        if (!player.isUsingItem()) {
            player.inventoryMenu.sendAllDataToRemote();
        }

        return interactionresultwrapper.getResult();
    }

    public InteractionResult useItemOn(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos blockposition = hitResult.getBlockPos();
        BlockState iblockdata = world.getBlockState(blockposition);
        InteractionResult enuminteractionresult = InteractionResult.PASS;
        boolean cancelledBlock = false;
        boolean cancelledItem = false; // Paper - correctly handle items on cooldown

        if (!iblockdata.getBlock().isEnabled(world.enabledFeatures())) {
            return InteractionResult.FAIL;
        } else if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider itileinventory = iblockdata.getMenuProvider(world, blockposition);
            cancelledBlock = !(itileinventory instanceof MenuProvider);
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            cancelledItem = true; // Paper - correctly handle items on cooldown
        }

        PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, blockposition, hitResult.getDirection(), stack, cancelledBlock, cancelledItem, hand, hitResult.getLocation()); // Paper - correctly handle items on cooldown
        this.firedInteract = true;
        this.interactResult = event.useItemInHand() == Event.Result.DENY;
        this.interactPosition = blockposition.immutable();
        this.interactHand = hand;
        this.interactItemStack = stack.copy();

        if (event.useInteractedBlock() == Event.Result.DENY) {
            // If we denied a door from opening, we need to send a correcting update to the client, as it already opened the door.
            if (!(iblockdata.getBlock() instanceof DoorBlock)) {
                if (iblockdata.getBlock() instanceof CakeBlock) {
                    player.getBukkitEntity().sendHealthUpdate(); // SPIGOT-1341 - reset health for cake
                } else if (!(this.interactItemStack.getItem() instanceof DoubleHighBlockItem)
                        && (iblockdata.getBlock() instanceof StructureBlock
                        || iblockdata.getBlock() instanceof CommandBlock))
                    player.connection.send(new ClientboundContainerClosePacket(this.player.containerMenu.containerId));
            }
            // Paper end - extend Player Interact cancellation
            player.getBukkitEntity().updateInventory(); // SPIGOT-2867
            this.player.resyncUsingItem(this.player); // Paper - Properly cancel usable items
            enuminteractionresult = (event.useItemInHand() != Event.Result.ALLOW) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } else if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider itileinventory = iblockdata.getMenuProvider(world, blockposition);

            if (itileinventory != null) {
                player.openMenu(itileinventory);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        } else {
            boolean flag = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
            boolean flag1 = player.isSecondaryUseActive() && flag;
            ItemStack itemstack1 = stack.copy();

            if (!flag1) {
                enuminteractionresult = iblockdata.use(world, player, hand, hitResult);

                if (enuminteractionresult.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockposition, itemstack1);
                    return enuminteractionresult;
                }
            }

            if (!stack.isEmpty() && enuminteractionresult != InteractionResult.SUCCESS && !this.interactResult) { // add !interactResult SPIGOT-764
                UseOnContext itemactioncontext = new UseOnContext(player, hand, hitResult);
                InteractionResult enuminteractionresult1;

                if (this.isCreative()) {
                    int i = stack.getCount();

                    enuminteractionresult1 = stack.useOn(itemactioncontext);
                    stack.setCount(i);
                } else {
                    enuminteractionresult1 = stack.useOn(itemactioncontext);
                }

                if (enuminteractionresult1.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockposition, itemstack1);
                }

                return enuminteractionresult1;
            }
            // Paper start - Properly cancel usable items; Cancel only if cancelled + if the interact result is different from default response
            else if (this.interactResult && this.interactResult != cancelledItem) {
                this.player.resyncUsingItem(this.player);
            }
            // Paper end - Properly cancel usable items
        }
        return enuminteractionresult;
        // CraftBukkit end
    }

    public void setLevel(ServerLevel world) {
        this.level = world;
    }
}
