package net.itfeng.mqttdemoclient.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import net.itfeng.mqttdemoclient.handler.MyMessageHandler;
import net.itfeng.mqttdemoclient.protocol.TestDataTransOuterClass;
import net.itfeng.mqttdemoclient.util.ClientIdUtil;
import net.itfeng.mqttdemoclient.util.MessageIdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * mqtt订阅者
 */
@Service
@Slf4j
public class MqttConnection {
    private MqttClient mqttClient;

    @Value("${mqtt.subscriber.brokers}")
    private String broker;

    @Value("${mqtt.subscriber.username: userNull}")
    private String userName;

    @Value("${mqtt.subscriber.password: passwordNull}")
    private String password;

    @Value("${mqtt.upload_online_msg_topic}")
    private String uploadOnlineMsgTopic;
    @Value("${mqtt.upload_heartbeat_topic}")
    private String uploadHeartbeatTopic;

    @Autowired
    private List<MyMessageHandler> myMessageHandlers;

    private static final AtomicBoolean connected = new AtomicBoolean(false);



    @PostConstruct
    public void start() throws MqttException {
        String clientId = "client_subscriber" + "_" + ClientIdUtil.getClientId(); // 你的客户端ID。
        // 创建一个新的MqttClient实例，使用MemoryPersistence作为存储引擎。
        mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
        // 创建连接选项并设置自动重新连接为true。
        MqttConnectOptions connOpts = new MqttConnectOptions();
        if(!"userNull".equals(userName)){
            connOpts.setUserName(userName);
            connOpts.setPassword(password.toCharArray());
        }
        connOpts.setAutomaticReconnect(true);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                connected.set(false);
                System.out.println("Connection lost. Reconnecting...");
                reconnect(connOpts);
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                MyMessageHandler handler= getMessageHandler(topic);
                if(handler!=null){
                    handler.handle(message.getPayload());
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Handle delivery complete
            }
        });
        // 连接到MQTT代理。
        reconnect(connOpts);
        // 开启心跳
        startHeartbeat();
    }

    @PreDestroy
    public void stop() throws MqttException {
        if (mqttClient != null) {
            mqttClient.disconnect();
            mqttClient.close();
        }
    }

    private void startHeartbeat() {
        // Start a thread to send heartbeat messages
        new Thread(() -> {
            while (true) {
                try {
                    if (connected.get()) {
                        TestDataTransOuterClass.HeartBeatPing ping = buildHeartBeatPing();
                        MqttMessage message = new MqttMessage(ping.toByteArray());
                        message.setQos(0);
                        // Send a heartbeat message
                        mqttClient.publish(String.format(uploadHeartbeatTopic,ClientIdUtil.getClientId()), message);
                        log.info("发送心跳 msg_id:{}, timestamp:{}",ping.getMsgId(), System.currentTimeMillis());
                    }
                    Thread.sleep(5000); // Send a heartbeat every 5 seconds
                } catch (MqttException | InterruptedException e) {
                    log.error("发送心跳异常", e);
                }
            }
        }).start();
    }
    private void publishOnlineEvent() throws MqttException {
        if (connected.get()) {

            // 构建上线事件
            MqttMessage message = new MqttMessage(buildOnlineEvent().toByteArray());
            message.setQos(0);
            mqttClient.publish(uploadOnlineMsgTopic, message);
            log.info("发送上线事件");
        }
    }
    private TestDataTransOuterClass.OnlineEvent buildOnlineEvent() {
        TestDataTransOuterClass.OnlineEvent.Builder builder = TestDataTransOuterClass.OnlineEvent.newBuilder();
        builder.setClientId(ClientIdUtil.getClientId())
                .setStartTimeMillis(System.currentTimeMillis())
                .setMsgId(MessageIdUtil.getMessageId());
        return builder.build();
    }
    private TestDataTransOuterClass.HeartBeatPing buildHeartBeatPing() {
        TestDataTransOuterClass.HeartBeatPing.Builder builder = TestDataTransOuterClass.HeartBeatPing.newBuilder();
        builder.setClientId(ClientIdUtil.getClientId())
                .setStartTimeMillis(System.currentTimeMillis())
                .setMsgId(MessageIdUtil.getMessageId());
        return builder.build();
    }

    private void reconnect(MqttConnectOptions connOpts) {
        while (!connected.get()) {
            try {
                mqttClient.connect(connOpts);
                connected.set(true);
                publishOnlineEvent();
                //  订阅主题
                myMessageHandlers.forEach(myMessageHandler -> {
                    try {
                        mqttClient.subscribe(myMessageHandler.getMessageType());
                        log.info("订阅topic:{}", myMessageHandler.getMessageType());
                    } catch (MqttException e) {
                        log.error("订阅topic异常"+ myMessageHandler.getMessageType());
                        throw new RuntimeException(e);
                    }
                });
            } catch (MqttException e) {
                log.error("Failed to reconnect. Trying again in 5 seconds.");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException interruptedException) {
                    log.error(interruptedException.getMessage(), interruptedException);
                }
            }
        }
    }

    private MyMessageHandler getMessageHandler(String topic){
        for (MyMessageHandler myMessageHandler : myMessageHandlers) {
            if(myMessageHandler.getMessageType().equals(topic)){
                return myMessageHandler;
            }
        }
        throw new RuntimeException("未找到对应的消息处理器");
    }


}

