package zip.sodium.delta.api.block;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zip.sodium.delta.api.DeltaPlugin;
import zip.sodium.delta.api.block.entity.TexturedDeltaBlockEntity;
import zip.sodium.delta.api.interfaces.DeltaBlock;
import zip.sodium.delta.helper.BlockHelper;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK;

public abstract class TexturedDeltaBlock extends Block implements DeltaBlock, EntityBlock {
    protected Display.ItemDisplay spawnSingleQuarter(final Level world,
                                                     final Vec3 pos,
                                                     final String texture) {
        final var quarter = EntityType.ITEM_DISPLAY.create(world);
        quarter.setItemStack(newPlayerHead(texture));

        quarter.setPos(pos);

        return quarter;
    }

    protected Display.ItemDisplay[] spawnQuarters(final Level world,
                                                  final BlockPos pos) {
        final var quarter1 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.25, 0.5, 0.25),
                texture1
        );

        final var quarter2 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.75, 0.5, 0.25),
                texture2
        );

        final var quarter3 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.25, 0.5, 0.75),
                texture3
        );

        final var quarter4 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.75, 0.5, 0.75),
                texture4
        );

        final var quarter5 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.75, 1, 0.25),
                texture5
        );

        final var quarter6 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.25, 1, 0.75),
                texture6
        );

        final var quarter7 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.75, 1, 0.75),
                texture7
        );

        final var quarter8 = spawnSingleQuarter(
                world,
                Vec3.atLowerCornerWithOffset(pos, 0.25, 1, 0.25),
                texture8
        );

        world.addFreshEntity(quarter1);
        world.addFreshEntity(quarter2);
        world.addFreshEntity(quarter3);
        world.addFreshEntity(quarter4);
        world.addFreshEntity(quarter5);
        world.addFreshEntity(quarter6);
        world.addFreshEntity(quarter7);
        world.addFreshEntity(quarter8);

        return new Display.ItemDisplay[] {
                quarter1,
                quarter2,
                quarter3,
                quarter4,
                quarter5,
                quarter6,
                quarter7,
                quarter8,
        };
    }

    protected ItemStack newPlayerHead(final String texture) {
        final var stack = new ItemStack(Items.PLAYER_HEAD);
        final var profile = new GameProfile(
                UUID.randomUUID(),
                "thisisdefinitelynotnull"
        );

        profile.getProperties().put("textures", new Property(
                "textures",
                texture
        ));

        stack.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), profile));

        return stack;
    }

    protected final String texture1;
    protected final String texture2;
    protected final String texture3;
    protected final String texture4;
    protected final String texture5;
    protected final String texture6;
    protected final String texture7;
    protected final String texture8;

    public TexturedDeltaBlock(
            final Properties settings,
            final String texture1,
            final String texture2,
            final String texture3,
            final String texture4,
            final String texture5,
            final String texture6,
            final String texture7,
            final String texture8) {
        super(settings);

        DeltaPlugin.TEXTURED_BLOCKS.add(this);

        this.texture1 = texture1;
        this.texture2 = texture2;
        this.texture3 = texture3;
        this.texture4 = texture4;
        this.texture5 = texture5;
        this.texture6 = texture6;
        this.texture7 = texture7;
        this.texture8 = texture8;
    }

    public TexturedDeltaBlock(
            final Properties settings,
            final String texture) {
        this(
                settings,
                texture,
                texture,
                texture,
                texture,
                texture,
                texture,
                texture,
                texture
        );
    }

    @Override
    protected void spawnDestroyParticles(final @NotNull Level world, final @NotNull Player player, final @NotNull BlockPos pos, final @NotNull BlockState state) {
        world.levelEvent(PARTICLES_DESTROY_BLOCK, pos, Block.getId(
                BlockHelper.getReplacementParticleBlockState(
                        (ServerPlayer) player,
                        state, pos
                )
        ));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final @NotNull BlockPos pos, final @NotNull BlockState state) {
        return new TexturedDeltaBlockEntity(pos, state);
    }

    @Override
    public void onPlace(final @NotNull BlockState state, final @NotNull Level world, final @NotNull BlockPos pos, final @NotNull BlockState oldState, final boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);

        final var be = DeltaBlock.<TexturedDeltaBlockEntity>getBlockEntityAt(world, pos);

        be.ids = Arrays.stream(spawnQuarters(
                world,
                pos
        )).map(Entity::getStringUUID).toList();
    }

    @Override
    public Block getDelta(@NotNull BlockState state, @Nullable ServerPlayer player) {
        return Blocks.BARRIER;
    }
}
