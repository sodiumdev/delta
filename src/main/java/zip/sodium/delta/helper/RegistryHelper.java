package zip.sodium.delta.helper;

import io.papermc.paper.util.ObfHelper;
import net.minecraft.core.IdMapper;
import net.minecraft.core.MappedRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers;
import zip.sodium.delta.agent.DeltaAgent;
import zip.sodium.delta.api.interfaces.DeltaBlock;
import zip.sodium.delta.api.interfaces.DeltaItem;
import zip.sodium.delta.blockstate.BlockStateIdMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegistryHelper {
    private RegistryHelper() {}

    private static final Field REGISTRY_FROZEN_FIELD;
    private static final Field REGISTRY_INTRUSIVE_HOLDERS_FIELD;

    private static final Map<Item, Material> ITEM_MATERIAL;
    private static final Map<Material, Item> MATERIAL_ITEM;

    private static final Map<Block, Material> BLOCK_MATERIAL;
    private static final Map<Material, Block> MATERIAL_BLOCK;

    static {
        try {
            REGISTRY_FROZEN_FIELD = MappingHelper.mapped(MappedRegistry.class, "frozen");
            REGISTRY_INTRUSIVE_HOLDERS_FIELD = MappingHelper.mapped(MappedRegistry.class, "unregisteredIntrusiveHolders");

            REGISTRY_FROZEN_FIELD.setAccessible(true);
            REGISTRY_INTRUSIVE_HOLDERS_FIELD.setAccessible(true);

            final var cmnimf = CraftMagicNumbers.class.getDeclaredField("ITEM_MATERIAL");
            cmnimf.setAccessible(true);

            final var cmnmif = CraftMagicNumbers.class.getDeclaredField("MATERIAL_ITEM");
            cmnmif.setAccessible(true);

            ITEM_MATERIAL = (Map<Item, Material>) cmnimf.get(null);
            MATERIAL_ITEM = (Map<Material, Item>) cmnmif.get(null);

            final var cmnbmf = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            cmnbmf.setAccessible(true);

            final var cmnmbf = CraftMagicNumbers.class.getDeclaredField("MATERIAL_BLOCK");
            cmnmbf.setAccessible(true);

            BLOCK_MATERIAL = (Map<Block, Material>) cmnbmf.get(null);
            MATERIAL_BLOCK = (Map<Material, Block>) cmnmbf.get(null);

            final var blockStateRegistry = MappingHelper.mapped(Block.class, "BLOCK_STATE_REGISTRY");
            blockStateRegistry.setAccessible(true);

            UnsafeHelper.setStaticField(blockStateRegistry, new BlockStateIdMapper((IdMapper<BlockState>) blockStateRegistry.get(null)));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            System.err.println("Welp, the mappings are fucked");

            throw new RuntimeException(e);
        }
    }

    /* Only here to load the class */
    public static void acknowledge() { }

    private static int itemCount = 0;
    public static void addItemToBukkit(final DeltaItem item) {
        final var cast = item.cast();

        if (ITEM_MATERIAL.containsKey(cast))
            return;

        final Material material;
        if (item instanceof BlockItem blockItem) {
            if (!BLOCK_MATERIAL.containsKey(blockItem.getBlock()))
                addBlockToBukkit((DeltaBlock) blockItem.getBlock());

            material = BLOCK_MATERIAL.get(blockItem.getBlock());
        } else material = Material.valueOf("DELTA_MAT_" + itemCount);

        ITEM_MATERIAL.put(cast, material);
        MATERIAL_ITEM.put(material, cast);

        itemCount++;
        if (itemCount > DeltaAgent.DIVIDER)
            throw new ArrayIndexOutOfBoundsException("Maximum registrable item count exceeded");
    }


    private static int blockCount = 0;
    public static void addBlockToBukkit(final DeltaBlock block) {
        final var cast = block.cast();

        if (BLOCK_MATERIAL.containsKey(cast))
            return;

        final var material = Material.valueOf("DELTA_MAT_" + (blockCount + DeltaAgent.DIVIDER));

        BLOCK_MATERIAL.put(cast, material);
        MATERIAL_BLOCK.put(material, cast);

        blockCount++;
        if (blockCount > DeltaAgent.DIVIDER)
            throw new ArrayIndexOutOfBoundsException("Maximum registrable block count exceeded");
    }

    public static boolean isFrozen(final MappedRegistry<?> registry) {
        try {
            return (Boolean) REGISTRY_FROZEN_FIELD.get(registry);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void unfreeze(final MappedRegistry<?> registry) {
        try {
            REGISTRY_FROZEN_FIELD.set(registry, false);
            REGISTRY_INTRUSIVE_HOLDERS_FIELD.set(registry, new HashMap<>());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
