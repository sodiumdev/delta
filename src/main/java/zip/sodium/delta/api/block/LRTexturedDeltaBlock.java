package zip.sodium.delta.api.block;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LRTexturedDeltaBlock extends TexturedDeltaBlock {
    public LRTexturedDeltaBlock(
            final Properties settings,
            final String texture) {
        super(settings,
                texture,
                texture,
                texture,
                texture,
                texture,
                texture,
                texture,
                texture);
    }

    @Override
    protected Display.ItemDisplay[] spawnQuarters(final Level world,
                                                  final BlockPos pos) {
        final var quarter = EntityType.ITEM_DISPLAY.create(world);
        quarter.setItemStack(newPlayerHead(texture1));

        quarter.setTransformation(
                new Transformation(
                        new Vector3f(),
                        new Quaternionf(),
                        new Vector3f(2, 2, 2),
                        new Quaternionf()
                )
        );
        quarter.setPos(Vec3.atLowerCornerWithOffset(pos, 0.5, 1, 0.5));

        world.addFreshEntity(quarter);

        return new Display.ItemDisplay[] {
                quarter
        };
    }
}
