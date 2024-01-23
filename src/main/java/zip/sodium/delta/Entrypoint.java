package zip.sodium.delta;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import zip.sodium.delta.api.Delta;
import zip.sodium.delta.api.DeltaPlugin;
import zip.sodium.delta.api.interfaces.DeltaBlock;
import zip.sodium.delta.api.interfaces.DeltaItem;
import zip.sodium.delta.api.item.DeltaBlockItem;
import zip.sodium.delta.builtin.block.HRRedMushroomBlock;
import zip.sodium.delta.builtin.block.LRRedMushroomBlock;
import zip.sodium.delta.builtin.item.LauncherItem;
import zip.sodium.delta.helper.LangHelper;
import zip.sodium.delta.helper.ResourceHelper;

import java.util.Locale;

public final class Entrypoint extends DeltaPlugin {
    public static Item LAUNCHER;

    public static Block HR_RED_MUSHROOM;
    public static Item HR_RED_MUSHROOM_ITEM;

    public static Block LR_RED_MUSHROOM;
    public static Item LR_RED_MUSHROOM_ITEM;

    @Override
    public void register() {
        LangHelper.register(Locale.US, "item.delta.launcher", "Launcher");

        LangHelper.register(Locale.US, "item.delta.hr_red_mushroom", "High Resolution Red Mushroom");
        LangHelper.register(Locale.US, "block.delta.hr_red_mushroom", "High Resolution Red Mushroom");

        LangHelper.register(Locale.US, "item.delta.lr_red_mushroom", "Low Resolution Red Mushroom");
        LangHelper.register(Locale.US, "block.delta.lr_red_mushroom", "Low Resolution Red Mushroom");

        Delta.tryRegister(() -> {
            Delta.register(
                    ResourceHelper.delta("launcher"),
                    (DeltaItem) (LAUNCHER = new LauncherItem(new Item.Properties().stacksTo(3)))
            );

            Delta.register(
                    ResourceHelper.delta("hr_red_mushroom"),
                    (DeltaBlock) (HR_RED_MUSHROOM = new HRRedMushroomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK)))
            );

            Delta.register(
                    ResourceHelper.delta("hr_red_mushroom"),
                    (DeltaItem) (HR_RED_MUSHROOM_ITEM = new DeltaBlockItem(Items.RED_MUSHROOM_BLOCK, HR_RED_MUSHROOM, new Item.Properties()))
            );

            Delta.register(
                    ResourceHelper.delta("lr_red_mushroom"),
                    (DeltaBlock) (LR_RED_MUSHROOM = new LRRedMushroomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK)))
            );

            Delta.register(
                    ResourceHelper.delta("lr_red_mushroom"),
                    (DeltaItem) (LR_RED_MUSHROOM_ITEM = new DeltaBlockItem(Items.RED_MUSHROOM_BLOCK, LR_RED_MUSHROOM, new Item.Properties()))
            );
        }, BuiltInRegistries.ITEM, BuiltInRegistries.BLOCK, BuiltInRegistries.BLOCK_ENTITY_TYPE);
    }
}
