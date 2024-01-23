package zip.sodium.delta.blockstate;

import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import zip.sodium.delta.helper.BlockHelper;
import zip.sodium.delta.helper.MappingHelper;
import zip.sodium.delta.helper.RegistryHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;

public final class BlockStateIdMapper extends IdMapper<BlockState> {
    private static final Field ID_TO_T_FIELD;
    private static final Field T_TO_ID_FIELD;
    private static final Field NEXT_ID_FIELD;

    static {
        try {
            ID_TO_T_FIELD = MappingHelper.mapped(IdMapper.class, "idToT");
            T_TO_ID_FIELD = MappingHelper.mapped(IdMapper.class, "tToId");
            NEXT_ID_FIELD = MappingHelper.mapped(IdMapper.class, "nextId");

            ID_TO_T_FIELD.setAccessible(true);
            T_TO_ID_FIELD.setAccessible(true);
            NEXT_ID_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            System.out.println("Welp, the mappings are fucked.");

            throw new RuntimeException(e);
        }
    }

    public BlockStateIdMapper(final IdMapper<BlockState> parent) {
        try {
            ID_TO_T_FIELD.set(this, ID_TO_T_FIELD.get(parent));
            T_TO_ID_FIELD.set(this, T_TO_ID_FIELD.get(parent));
            NEXT_ID_FIELD.set(this, NEXT_ID_FIELD.get(parent));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getId(@NotNull BlockState value) {
        return super.getId(
                BlockHelper.getReplacementBlockState(
                        value, null
                )
        );
    }
}
