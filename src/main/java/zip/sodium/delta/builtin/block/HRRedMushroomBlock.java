package zip.sodium.delta.builtin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zip.sodium.delta.api.block.TexturedDeltaBlock;

public final class HRRedMushroomBlock extends TexturedDeltaBlock {
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzMyZGJkNjYxMmU5ZDNmNDI5NDdiNWNhODc4NWJmYjMzNDI1OGYzY2ViODNhZDY5YTVjZGVlYmVhNGNkNjUifX19";

    public HRRedMushroomBlock(Properties settings) {
        super(settings, TEXTURE);
    }

    @Override
    public BlockState getParticleReplacement(@Nullable ServerPlayer player, @NotNull BlockState state, @NotNull BlockPos pos) {
        return Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
    }
}
