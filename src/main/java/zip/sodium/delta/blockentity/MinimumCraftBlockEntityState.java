package zip.sodium.delta.blockentity;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockEntityState;
import org.jetbrains.annotations.NotNull;

public class MinimumCraftBlockEntityState<T extends BlockEntity> extends CraftBlockEntityState<T> {
    public MinimumCraftBlockEntityState(World world, T tileEntity) {
        super(world, tileEntity);
    }

    protected MinimumCraftBlockEntityState(CraftBlockEntityState<T> state) {
        super(state);
    }

    @Override
    public @NotNull CraftBlockEntityState<T> copy() {
        return new MinimumCraftBlockEntityState<>(this);
    }
}
