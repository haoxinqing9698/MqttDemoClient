# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.1/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.1/maven-plugin/reference/html/#build-image)

### 项目环境

* JDK 17
* spring-boot 3.2.1
* mqtt
* 多线程

### 项目说明

* 测试数据使用protobuf编码压缩
* mqtt服务有密码直接配置用户名和密码，无用户则不需要配置
* 本客户端与服务端配合使用


### 配置说明
* 下载好项目后配置jdk17环境
* 配置后先执行clean，然后执行maven Plugins中的 protoc-jar:run生成pb文件
* 配置src/main/generated-sources目录为Generated Sources Root

### 业务文档
* 见服务端项目文档

### 业务流程技术说明
* 系统启动时，加载配置信息[application.yml](src%2Fmain%2Fresources%2Fapplication.yml)，并将监听的topic注入到对应的topic消息处理器中，详情见MyMessageHandler及其实现类
* 系统启动后在MqttConnection中订阅mqtt的topic, 包括业务心跳响应topic，服务端下发端时间差校准topic(用于计算客户端与服务端的时间差毫秒值)，测试数据传输topic(用于接收测试数据)
* 系统启动后，发送一个上线事件到mqtt的上线事件topic中，然后开始发送心跳数据
* 档收到时间校准数据后，立即响应一条时间校准响应数据
* 当收到测试数据后，将向服务端响应一条测试数据响应