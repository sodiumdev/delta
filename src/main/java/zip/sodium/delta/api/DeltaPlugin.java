package zip.sodium.delta.api;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;
import zip.sodium.delta.api.block.TexturedDeltaBlock;
import zip.sodium.delta.api.block.entity.TexturedDeltaBlockEntity;
import zip.sodium.delta.helper.BlockEntityHelper;
import zip.sodium.delta.helper.ResourceHelper;
import zip.sodium.delta.helper.UnsafeHelper;
import zip.sodium.delta.listener.ConnectionListener;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public abstract class DeltaPlugin extends JavaPlugin {
    public static final Set<TexturedDeltaBlock> TEXTURED_BLOCKS = new HashSet<>();

    private static BlockEntityType<?> TEXTURED_DELTA_BLOCK_ENTITY;
    public static BlockEntityType<?> getTexturedDeltaBlockEntity() {
        return TEXTURED_DELTA_BLOCK_ENTITY;
    }

    protected void load() {}
    protected void register() {}
    protected void enable() {}
    protected void disable() {}

    @Override
    public final void onLoad() {
        load();
        register();

        Delta.tryRegister(() -> Delta.register(
                ResourceHelper.delta("textured_delta"),
                TEXTURED_DELTA_BLOCK_ENTITY = BlockEntityHelper.createType(
                        TexturedDeltaBlockEntity::new,
                        TEXTURED_BLOCKS.toArray(Block[]::new)
                )
        ), BuiltInRegistries.BLOCK_ENTITY_TYPE);
    }

    @Override
    public final void onEnable() {
        getServer().getPluginManager().registerEvents(new ConnectionListener(), this);

        enable();
    }

    @Override
    public final void onDisable() {
        disable();
    }
}
