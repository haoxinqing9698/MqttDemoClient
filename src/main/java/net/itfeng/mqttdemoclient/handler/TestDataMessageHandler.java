package net.itfeng.mqttdemoclient.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import net.itfeng.mqttdemoclient.protocol.TestDataTransOuterClass;
import net.itfeng.mqttdemoclient.service.MessageAsyncPublishService;
import net.itfeng.mqttdemoclient.util.ClientIdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 测试消息处理类
 *
 * @author fengxubo
 * @since 2024/1/27 10:13
 * 
 */
@Service
public class TestDataMessageHandler implements MyMessageHandler{

    @Value("${mqtt.push_msg_topic}")
    private String pushMsgTopic;
    private String pushMsgTopicWithClientId;

    @Value("${mqtt.upload_msg_result_topic}")
    private String uploadMsgResultTopic;
    private String uploadMsgResultTopicWithClientId;

    @Autowired
    private MessageAsyncPublishService messageAsyncPublishService;
    @Async("mqttMessageHandlerPool")
    @Override
    public void handle(byte[] messageBytes) {
        try {
            TestDataTransOuterClass.TestDataTrans testData = TestDataTransOuterClass.TestDataTrans.parseFrom(messageBytes);
            if(testData != null) {
                // 发送一个result，然后开始处理
                messageAsyncPublishService.publish(getUploadMsgResultTopic(),buildSuccessResult(testData).toByteArray());
            }else{
                // 发送一个消息为空的result
                messageAsyncPublishService.publish(getUploadMsgResultTopic(),buildResult("TEST_DATA_NULL").toByteArray());
            }
        } catch (InvalidProtocolBufferException e) {
            // 发送一个解析失败的result
            messageAsyncPublishService.publish(getUploadMsgResultTopic(),buildResult("PB_ERROR").toByteArray());
            throw new RuntimeException(e);
        }
    }

    private String getUploadMsgResultTopic(){
        if(uploadMsgResultTopicWithClientId == null){
            uploadMsgResultTopicWithClientId = String.format(uploadMsgResultTopic, ClientIdUtil.getClientId());
        }
        return uploadMsgResultTopicWithClientId;
    }

    private TestDataTransOuterClass.TestDataTransResult buildSuccessResult(TestDataTransOuterClass.TestDataTrans testData) {
        return TestDataTransOuterClass.TestDataTransResult.newBuilder()
                .setStartTimeMillis(testData.getStartTimeMillis())
                .setClientId(testData.getClientId())
                .setMsgId(testData.getMsgId())
                .setResult("SUCCESS")
                .setReceivedTimeMillis(System.currentTimeMillis())
                .build();
    }

    private TestDataTransOuterClass.TestDataTransResult buildResult(String info) {
        return TestDataTransOuterClass.TestDataTransResult.newBuilder()
        .setResult(info).setReceivedTimeMillis(System.currentTimeMillis()).build();
    }

    @Override
    public String getMessageType() {
        if(pushMsgTopicWithClientId == null){
            pushMsgTopicWithClientId = String.format(pushMsgTopic, ClientIdUtil.getClientId());
        }
        return pushMsgTopicWithClientId;
    }
}
