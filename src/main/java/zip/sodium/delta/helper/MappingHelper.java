package zip.sodium.delta.helper;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.IdMapper;
import net.minecraft.core.MappedRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public final class MappingHelper {
    private MappingHelper() {}

    private static final Map<Class<?>, List<Pair<String, String>>> MAPPINGS = new HashMap<>();
    public static Field mapped(final Class<?> clazz, final String fieldName) throws NoSuchFieldException {
        Field field;

        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            field = clazz.getDeclaredField(
                    MAPPINGS.get(clazz)
                            .stream()
                            .filter(x -> x.key().equals(fieldName))
                            .findFirst()
                            .get()
                            .value()
            );
        }

        return field;
    }

    static {
        MAPPINGS.put(
                MappedRegistry.class,
                List.of(
                        Pair.of(
                                "frozen",
                                "l"
                        ),

                        Pair.of(
                                "unregisteredIntrusiveHolders",
                                "m"
                        )
                )
        );

        MAPPINGS.put(
                Block.class,
                List.of(
                        Pair.of(
                                "BLOCK_STATE_REGISTRY",
                                "q"
                        )
                )
        );

        MAPPINGS.put(
                IdMapper.class,
                List.of(
                        Pair.of(
                                "nextId",
                                "b"
                        ),
                        Pair.of(
                                "tToId",
                                "c"
                        ),
                        Pair.of(
                                "idToT",
                                "d"
                        )
                )
        );

        MAPPINGS.put(
                ServerPlayer.class,
                List.of(
                        Pair.of(
                                "gameMode",
                                "e"
                        )
                )
        );
    }
}
