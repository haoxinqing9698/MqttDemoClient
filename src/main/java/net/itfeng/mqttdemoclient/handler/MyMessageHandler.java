package net.itfeng.mqttdemoclient.handler;

/**
 * 消息处理接口
 *
 * @author fengxubo
 * @since 2024/1/26 17:41
 * 
 */
public interface MyMessageHandler {
    void handle(byte[] messageBytes);

    String getMessageType();
}
