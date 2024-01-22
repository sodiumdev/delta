package zip.sodium.delta;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.bukkit.Material;
import zip.sodium.delta.api.Delta;
import zip.sodium.delta.api.DeltaPlugin;
import zip.sodium.delta.api.interfaces.DeltaBlock;
import zip.sodium.delta.api.interfaces.DeltaItem;
import zip.sodium.delta.api.item.DeltaBlockItem;
import zip.sodium.delta.builtin.block.BallsaltBlock;
import zip.sodium.delta.builtin.item.LauncherItem;
import zip.sodium.delta.helper.LangHelper;
import zip.sodium.delta.helper.ResourceHelper;

import java.util.Arrays;
import java.util.Locale;

public final class Entrypoint extends DeltaPlugin {
    public static Item LAUNCHER;

    public static Block BALLSALT;
    public static Item BALLSALT_ITEM;

    @Override
    public void register() {
        LangHelper.register(Locale.US, "item.delta.launcher", "Launcher");

        LangHelper.register(Locale.US, "item.delta.ballsalt", "Ballsalt");
        LangHelper.register(Locale.US, "block.delta.ballsalt", "Ballsalt");

        Delta.tryRegister(() -> {
            Delta.register(
                    ResourceHelper.delta("launcher"),
                    (DeltaItem) (LAUNCHER = new LauncherItem(new Item.Properties().stacksTo(3)))
            );

            Delta.register(
                    ResourceHelper.delta("ballsalt"),
                    (DeltaBlock) (BALLSALT = new BallsaltBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BASALT)))
            );

            Delta.register(
                    ResourceHelper.delta("ballsalt"),
                    (DeltaItem) (BALLSALT_ITEM = new DeltaBlockItem(Items.BASALT, BALLSALT, new Item.Properties()))
            );
        }, BuiltInRegistries.ITEM, BuiltInRegistries.BLOCK, BuiltInRegistries.BLOCK_ENTITY_TYPE);
    }
}
