package zip.sodium.delta.packet;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolSwapHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public final class DeltaPacketDecoder extends ByteToMessageDecoder implements ProtocolSwapHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AttributeKey<ConnectionProtocol.CodecData<?>> CODEC_KEY = Connection.ATTRIBUTE_SERVERBOUND_PROTOCOL;

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        int readableBytes = byteBuf.readableBytes();
        if (readableBytes == 0)
            return;

        final Attribute<ConnectionProtocol.CodecData<?>> attribute = channelHandlerContext.channel().attr(CODEC_KEY);
        final ConnectionProtocol.CodecData<?> codecData = attribute.get();

        final var player = ((Connection) channelHandlerContext.channel().pipeline().get("packet_handler")).getPlayer();

        final var buf = new DeltaByteBuf(player, byteBuf);

        final int packetId = buf.readVarInt();
        final Packet<?> packet = codecData.createPacket(packetId, buf);
        if (packet == null)
            throw new IOException("Bad packet id " + packetId);

        JvmProfiler.INSTANCE.onPacketReceived(codecData.protocol(), packetId, channelHandlerContext.channel().remoteAddress(), readableBytes);
        if (buf.readableBytes() > 0)
            throw new IOException("Packet " + codecData.protocol().id() + "/" + packetId + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + buf.readableBytes() + " bytes extra whilst reading packet " + packetId);

        list.add(packet);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug(Connection.PACKET_RECEIVED_MARKER, " IN: [{}:{}] {}", codecData.protocol().id(), packetId, packet.getClass().getName());

        ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet);
    }
}
