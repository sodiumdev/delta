package zip.sodium.delta.listener;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import zip.sodium.delta.helper.MappingHelper;
import zip.sodium.delta.helper.UnsafeHelper;
import zip.sodium.delta.packet.DeltaPacketDecoder;
import zip.sodium.delta.packet.DeltaPacketEncoder;
import zip.sodium.delta.player.DeltaSPGM;

import java.lang.reflect.Field;

public final class ConnectionListener implements Listener {
    private static final Field SERVER_PLAYER_GAME_MODE_FIELD;

    static {
        try {
            SERVER_PLAYER_GAME_MODE_FIELD = MappingHelper.mapped(ServerPlayer.class, "gameMode");
            SERVER_PLAYER_GAME_MODE_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            System.err.println("Welp, the mappings are fucked");

            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        final var player = ((CraftPlayer) e.getPlayer()).getHandle();
        final var channel = player.connection.connection.channel;

        channel.eventLoop().submit(() -> {
            channel.pipeline().replace("encoder", "encoder", new DeltaPacketEncoder());
            channel.pipeline().replace("decoder", "decoder", new DeltaPacketDecoder());
        });

        UnsafeHelper.setField(player, SERVER_PLAYER_GAME_MODE_FIELD, new DeltaSPGM(player, (ServerPlayerGameMode) UnsafeHelper.getObject(player, SERVER_PLAYER_GAME_MODE_FIELD)));
    }
}
