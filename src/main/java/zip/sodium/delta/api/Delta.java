package zip.sodium.delta.api;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockStates;
import org.jetbrains.annotations.NotNull;
import zip.sodium.delta.blockentity.MinimumCraftBlockEntityState;
import zip.sodium.delta.helper.BlockEntityHelper;
import zip.sodium.delta.helper.RegistryHelper;
import zip.sodium.delta.api.interfaces.DeltaBlock;
import zip.sodium.delta.api.interfaces.DeltaItem;
import zip.sodium.delta.api.interfaces.DeltaObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

public final class Delta {
    private Delta() {}

    private static final Method REGISTER_BLOCK_ENTITY_METHOD;

    static {
        try {
            REGISTER_BLOCK_ENTITY_METHOD = CraftBlockStates.class.getDeclaredMethod("register", BlockEntityType.class, Class.class, BiFunction.class);

            REGISTER_BLOCK_ENTITY_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            System.err.println("Welp, the mappings are fucked");

            throw new RuntimeException(e);
        }
    }

    public static <T> void register(final @NotNull ResourceLocation id, final DeltaObject<T, ?> item) {
        Registry.register(item.getRegistry(), id, item.cast());

        if (item instanceof DeltaItem deltaItem)
            RegistryHelper.addItemToBukkit(deltaItem);
        else if (item instanceof DeltaBlock deltaBlock)
            RegistryHelper.addBlockToBukkit(deltaBlock);
    }

    public static <T extends BlockEntity> void register(final @NotNull ResourceLocation id, final BlockEntityType<?> item) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, item);

        try {
            REGISTER_BLOCK_ENTITY_METHOD.invoke(null, item, MinimumCraftBlockEntityState.class, (BiFunction<World, T, CraftBlockEntityState<T>>) MinimumCraftBlockEntityState::new);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void tryRegister(final Runnable runnable, final Registry<?>... registries) {
        for (final Registry<?> registry : registries) {
            final var reg = (MappedRegistry<?>) registry;

            if (RegistryHelper.isFrozen(reg))
                RegistryHelper.unfreeze(reg);
        }

        try {
            runnable.run();
        } finally {
            for (final Registry<?> registry : registries)
                registry.freeze();
        }
    }

    public static void init() {
        RegistryHelper.acknowledge();
        BlockEntityHelper.acknowledge();
    }
}
