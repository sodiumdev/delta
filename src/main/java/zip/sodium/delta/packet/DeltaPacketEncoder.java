package zip.sodium.delta.packet;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolSwapHandler;
import net.minecraft.network.SkipPacketException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

import java.io.IOException;

/* Credits to Bukkit and Paper and of course Minecraft */
public final class DeltaPacketEncoder extends MessageToByteEncoder<Packet<?>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final AttributeKey<ConnectionProtocol.CodecData<?>> CODEC_KEY = Connection.ATTRIBUTE_CLIENTBOUND_PROTOCOL;
    private static final int MAX_PACKET_SIZE = 8388608;

    protected void encode(ChannelHandlerContext channelHandlerContext, Packet<?> packet, ByteBuf byteBuf) throws Exception {
        final Attribute<ConnectionProtocol.CodecData<?>> attribute = channelHandlerContext.channel().attr(CODEC_KEY);
        final ConnectionProtocol.CodecData<?> codecData = attribute.get();
        if (codecData == null)
            throw new RuntimeException("ConnectionProtocol unknown: " + packet);

        final int packetId = codecData.packetId(packet);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug(Connection.PACKET_SENT_MARKER, "OUT: [{}:{}] {}", codecData.protocol().id(), packetId, packet.getClass().getName());

        if (packetId == -1)
            throw new IOException("Can't serialize unregistered packet");

        final var player = ((Connection) channelHandlerContext.channel().pipeline().get("packet_handler")).getPlayer();

        final var buf = new DeltaByteBuf(player, byteBuf);
        buf.writeVarInt(packetId);
        buf.adventure$locale = channelHandlerContext.channel().attr(io.papermc.paper.adventure.PaperAdventure.LOCALE_ATTRIBUTE).get(); // Paper - adventure; set player's locale

        if (packet instanceof ClientboundLevelEventPacket levelPacket
                && levelPacket.getType() == 2001)
            packet = new ClientboundLevelEventPacket(
                    levelPacket.getType(),
                    levelPacket.getPos(),
                    levelPacket.getData(),
                    levelPacket.isGlobalEvent()
            );

        try {
            final int packetBufferStart = buf.writerIndex();
            packet.write(buf);
            final int packetBufferLength = buf.writerIndex() - packetBufferStart;

            JvmProfiler.INSTANCE.onPacketSent(codecData.protocol(), packetId, channelHandlerContext.channel().remoteAddress(), packetBufferLength);
        } catch (Throwable throwable) {
            var packetName = io.papermc.paper.util.ObfHelper.INSTANCE.deobfClassName(packet.getClass().getName());
            if (packetName.contains("."))
                packetName = packetName.substring(packetName.lastIndexOf(".") + 1);

            LOGGER.error("Packet encoding of packet {} (ID: {}) threw (skippable? {})", packetName, packetId, packet.isSkippable(), throwable);

            if (packet.isSkippable())
                throw new SkipPacketException(throwable);

            throw throwable;
        } finally {
            final int packetLength = buf.readableBytes();
            if (packetLength > MAX_PACKET_SIZE)
                throw new PacketTooLargeException(packet, CODEC_KEY, packetLength);

            ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet);
        }
    }

    public static class PacketTooLargeException extends RuntimeException {
        private final Packet<?> packet;
        public final AttributeKey<ConnectionProtocol.CodecData<?>> codecKey;

        PacketTooLargeException(Packet<?> packet, AttributeKey<ConnectionProtocol.CodecData<?>> codecKey, int packetLength) {
            super("PacketTooLarge - " + packet.getClass().getSimpleName() + " is " + packetLength + ". Max is " + MAX_PACKET_SIZE);
            this.packet = packet;
            this.codecKey = codecKey;
        }

        public Packet<?> getPacket() {
            return this.packet;
        }
    }
}