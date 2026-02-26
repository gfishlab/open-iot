package com.openiot.connect.netty;

import com.openiot.connect.auth.DeviceAuthService;
import com.openiot.connect.protocol.ProtocolAdapter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TCP 消息处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class TcpMessageHandler extends SimpleChannelInboundHandler<String> {

    private final DeviceAuthService authService;
    private final ProtocolAdapter protocolAdapter;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        log.debug("收到TCP消息: {}", msg);

        try {
            // 解析消息
            ProtocolAdapter.ParseResult result = protocolAdapter.parse(msg);

            if (!result.isSuccess()) {
                log.warn("消息解析失败: {}", result.getError());
                ctx.writeAndFlush("ERROR:" + result.getError());
                return;
            }

            // 验证设备身份
            if (!authService.authenticate(result.getDeviceToken())) {
                log.warn("设备认证失败: token={}", result.getDeviceToken());
                ctx.writeAndFlush("ERROR:AUTH_FAILED");
                ctx.close();
                return;
            }

            // 发送到 Kafka
            protocolAdapter.sendToKafka(result);

            ctx.writeAndFlush("OK");

        } catch (Exception e) {
            log.error("处理消息异常", e);
            ctx.writeAndFlush("ERROR:" + e.getMessage());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新的TCP连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("TCP连接断开: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("TCP连接异常: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
