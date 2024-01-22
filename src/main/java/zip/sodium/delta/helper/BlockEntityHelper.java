package zip.sodium.delta.helper;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class BlockEntityHelper {
    private BlockEntityHelper() {}

    private static Class<?> FACTORY_CLASS;
    private static Constructor<?> BLOCK_ENTITY_TYPE_FACTORY;


    static {
        FACTORY_CLASS = Arrays.stream(BlockEntityType.class.getDeclaredClasses()).filter(x -> x.isInterface() && !Modifier.isPublic(x.getModifiers())).findAny().get();

        BLOCK_ENTITY_TYPE_FACTORY = BlockEntityType.class.getDeclaredConstructors()[0];
    }

    /* Only here to load the class */
    public static void acknowledge() {}

    public static <T extends BlockEntity> BlockEntityType<T> createType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        try {
            return (BlockEntityType<T>) BLOCK_ENTITY_TYPE_FACTORY.newInstance(
                    Proxy.newProxyInstance(
                            FACTORY_CLASS.getClassLoader(),
                            new Class[] { FACTORY_CLASS },
                            (proxy, method, args) -> {
                                final var parameters = method.getParameterTypes();

                                if (parameters.length != 2 || parameters[0] != BlockPos.class || parameters[1] != BlockState.class)
                                    return null;

                                return factory.apply((BlockPos) args[0], (BlockState) args[1]);
                            }
                    ),
                    Set.of(blocks),
                    null
            );
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
