package net.itfeng.mqttdemoclient.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import net.itfeng.mqttdemoclient.protocol.TestDataTransOuterClass;
import net.itfeng.mqttdemoclient.util.ClientIdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 时间校准消息处理器
 *
 * @author fengxubo
 * @since 2024/1/8 15:46
 * 
 */
@Slf4j
@Service
public class HeartbeatPoneMessageHandler implements MyMessageHandler{

    /**
     * 数据上传时间差主题, 客户端发送给服务端，topic中包含占位符，需要使用client_id替换
     */
    @Value("${mqtt.push_heartbeat_pong_topic}")
    private String heartbeatPoneTopic;
    private String heartbeatPoneTopicWithClientId;



    @Async("mqttMessageHandlerPool")
    public void handle(byte[] messageBytes)  {
        TestDataTransOuterClass.HeartBeatPong heartBeatPong = null;
        try {
            heartBeatPong = TestDataTransOuterClass.HeartBeatPong.parseFrom(messageBytes);
        } catch (InvalidProtocolBufferException e) {
            log.error("解析心跳响应消息失败", e);
        }
        log.info("收到心跳响应消息, msg_id:{}, 服务器时间:{}", heartBeatPong.getMsgId(), heartBeatPong.getReceivedTimeMillis());
    }

    @Override
    public String getMessageType() {
        if (heartbeatPoneTopicWithClientId == null) {
            String clientId = ClientIdUtil.getClientId();
            this.heartbeatPoneTopicWithClientId = String.format(heartbeatPoneTopic, clientId);
        }
        return this.heartbeatPoneTopicWithClientId;
    }
}
