package com.openiot.connect.netty;

import com.openiot.connect.auth.DeviceAuthService;
import com.openiot.connect.metrics.ConnectMetrics;
import com.openiot.connect.protocol.ProtocolAdapter;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TCP 消息处理器
 *
 * <p>处理设备上报的 TCP 消息，包括：
 * <ul>
 *   <li>消息解析</li>
 *   <li>设备认证</li>
 *   <li>数据转发到 Kafka</li>
 *   <li>指标收集</li>
 * </ul>
 *
 * @author OpenIoT Team
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class TcpMessageHandler extends SimpleChannelInboundHandler<String> {

    private final DeviceAuthService authService;
    private final ProtocolAdapter protocolAdapter;
    private final ConnectMetrics connectMetrics;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg == null || msg.isBlank()) {
            ctx.writeAndFlush("ERROR:EMPTY_MESSAGE");
            return;
        }
        log.debug("收到TCP消息: {}", msg);

        // 开始计时
        Timer.Sample timerSample = connectMetrics.startTimer();
        String ruleType = "PRIVATE"; // 私有协议

        try {
            // 解析消息
            ProtocolAdapter.ParseResult result = protocolAdapter.parse(msg);

            if (!result.isSuccess()) {
                log.warn("消息解析失败: {}", result.getError());
                connectMetrics.recordParseRuleExecution(
                        timerSample.stop(connectMetrics.getParseRuleExecutionTimer()),
                        ruleType, false);
                ctx.writeAndFlush("ERROR:" + result.getError());
                return;
            }

            // 记录解析成功
            long parseDuration = System.currentTimeMillis();
            connectMetrics.recordParseRuleExecution(parseDuration, ruleType, true);

            // 验证设备身份
            if (!authService.authenticate(result.getDeviceToken())) {
                log.warn("设备认证失败: token={}", result.getDeviceToken());
                ctx.writeAndFlush("ERROR:AUTH_FAILED");
                ctx.close();
                return;
            }

            // 发送到 Kafka（转发指标在 ProtocolAdapter 中记录）
            protocolAdapter.sendToKafka(result);

            ctx.writeAndFlush("OK");

        } catch (Exception e) {
            log.error("处理消息异常", e);
            connectMetrics.recordParseRuleExecution(
                    timerSample.stop(connectMetrics.getParseRuleExecutionTimer()),
                    ruleType, false);
            ctx.writeAndFlush("ERROR:" + e.getMessage());
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // 出站缓冲区高水位时暂停读取，防止内存持续上涨
        boolean writable = ctx.channel().isWritable();
        ctx.channel().config().setAutoRead(writable);
        if (!writable) {
            log.warn("Channel不可写，暂停读取: {}", ctx.channel().remoteAddress());
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新的TCP连接: {}", ctx.channel().remoteAddress());
        // 记录设备连接（协议类型 TCP，租户信息在认证后获取）
        connectMetrics.recordDeviceConnected("unknown", "pending", "TCP");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("TCP连接断开: {}", ctx.channel().remoteAddress());
        // 记录设备断开
        connectMetrics.recordDeviceDisconnected("unknown", "unknown", "connection_closed");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("TCP连接异常: {}", ctx.channel().remoteAddress(), cause);
        // 记录设备断开（异常原因）
        connectMetrics.recordDeviceDisconnected("unknown", "unknown", "exception");
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("读空闲超时，关闭连接: {}", ctx.channel().remoteAddress());
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.debug("写空闲超时: {}", ctx.channel().remoteAddress());
            } else if (event.state() == IdleState.ALL_IDLE) {
                log.warn("全空闲超时，关闭连接: {}", ctx.channel().remoteAddress());
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
