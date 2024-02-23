package net.itfeng.mqttdemoclient.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import net.itfeng.mqttdemoclient.protocol.TestDataTransOuterClass;
import net.itfeng.mqttdemoclient.service.MessageAsyncPublishService;
import net.itfeng.mqttdemoclient.util.ClientIdUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TimeDiffMessageHandler implements MyMessageHandler{

    @Autowired
    private MessageAsyncPublishService messageAsyncPublishService;


    @Value("${mqtt.push_time_diff_topic}")
    private String timeDiffTopic;
    /**
     * 数据上传时间差主题, 客户端发送给服务端，topic中包含占位符，需要使用client_id替换
     */
    @Value("${mqtt.upload_time_diff_topic}")
    private String uploadTimeDiffTopic;
    private String uploadTimeDiffTopicWithClientId;



    @Async("mqttMessageHandlerPool")
    public void handle(byte[] messageBytes)  {
        long now = System.currentTimeMillis();
        String sn = ClientIdUtil.getClientId();
        TestDataTransOuterClass.ClientTimeDiff clientTimeDiff;
        try {
            clientTimeDiff = TestDataTransOuterClass.ClientTimeDiff.parseFrom(messageBytes);
        } catch (InvalidProtocolBufferException e) {
            log.error("ClientTimeDiff 反序列化异常", e);
            throw new RuntimeException(e);
        }
        TestDataTransOuterClass.ClientTimeDiff clientTimeDiffNew = TestDataTransOuterClass.ClientTimeDiff.newBuilder()
                .setClientId(sn)
                .setMsgId(clientTimeDiff.getMsgId())
                .setStartTimeMillis(clientTimeDiff.getStartTimeMillis())
                .setTimeMillis(now)
                .build();
        // 客户端收到时间校准消息，发送给云服务
        messageAsyncPublishService.publish(getUploadTimeDiffTopicWithClientId(), clientTimeDiffNew.toByteArray());
    }

    private String getUploadTimeDiffTopicWithClientId() {
        if (uploadTimeDiffTopicWithClientId == null) {
            String clientId = ClientIdUtil.getClientId();
            this.uploadTimeDiffTopicWithClientId = String.format(uploadTimeDiffTopic, clientId);
        }
        return this.uploadTimeDiffTopicWithClientId;
    }

    @Override
    public String getMessageType() {
        return timeDiffTopic;
    }
}
