package zip.sodium.delta.builtin.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import zip.sodium.delta.api.interfaces.DeltaItem;

public class LauncherItem extends Item implements DeltaItem {
    public LauncherItem(Properties settings) {
        super(settings);
    }

    @Override
    public Item getDelta(@NotNull ItemStack stack, @Nullable ServerPlayer player) {
        return Items.STICK;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        final Block block = player.getBukkitEntity().getTargetBlock(null, 5);

        if (block.getType().isEmpty())
            return InteractionResultHolder.fail(player.getItemInHand(hand));

        final var world = (ServerLevel) level;

        var from = new Vec3(
                player.getX(),
                player.getY() + (double) player.getEyeHeight() / 2,
                player.getZ()
        );
        final var to = CraftLocation.toVec3D(block.getLocation().toCenterLocation());

        final double distance = from.distanceTo(to);

        final double spacing = 0.125;
        final var vec = from.subtract(to).normalize().multiply(spacing, spacing, spacing);

        final var particleType = ParticleTypes.SOUL_FIRE_FLAME;

        for (double length = 0; length < distance; from = from.subtract(vec)) {
            world.sendParticles(
                    particleType,
                    from.x,
                    from.y,
                    from.z,
                    1,
                    0, 0, 0,
                    0
            );

            length += spacing;
        }

        world.sendParticles(
                particleType,
                to.x,
                to.y,
                to.z,
                50,
                0.0025, 0.0025, 0.0025,
                0.25
        );

        world.explode(null, world.damageSources().generic(), null, block.getX(), block.getY(), block.getZ(), 1, false, Level.ExplosionInteraction.BLOCK, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.SOUL_FIRE_FLAME, SoundEvents.GENERIC_EXPLODE);
        block.setType(Material.AIR);

        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
