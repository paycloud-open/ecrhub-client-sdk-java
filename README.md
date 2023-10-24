# ECRHubClient SDK for Java

## About this sdk
Help Java developers quickly use ECRHubClient SDK to achieve LAN cross-device service calls. Developers do not need to care about the complexity of the internal logic , just call a method to complete a functional operation .

## Features
- **Multiple connection mode:** This SDK provides multiple ways to connect to WiseCashier, including serial port and websocket. You can choose the applicable connection method according to your ECR system environment.
- **Payments type supported:** This SDK provides a variety of payment methods, including bank card code scanning, and also provides a lot of intermediary payment capabilities, such as purchase, refund, query, close, pre-authorization, pre-authorization completion and so on.
- **Automatic pair:** This SDK automatically pairs by default, ECR developers do not need to develop additional pairing functions and pairing management interface.
- **USB serial port：**
    - Automatic port discovery: This SDK supports automatic serial port discovery capability. When creating ECRHubClient, if the serial port name is: "sp://", the SDK will automatically find the available serial ports and then connect them.
    - Automatic reconnection: This SDK supports heartbeat connection detection, detecting heartbeat once every 30 seconds, if the heartbeat request is not received from ECRHub, it will disconnect and wait for the next transaction request to automatically establish a connection and initiate the transaction.


## Getting Started

### 1. Requirements
- Suitable for Java language, JDK version 1.8 and above development environment.

### 2. Steps
- Download the jar package and add it to your project, please refer to github source code. <a href = "https://github.com/paycloud-open/ecrhub-client-sdk-java" target = "_blank">ecrhub-client-sdk-java</a>
- This JAR depends on some open source third party JARs. If these JARs are not integrated in your project, you will need to manually add dependencies to your project.

```XML
<!-- Mandatory -->
<!-- jSerialComm -->
<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>[2.0.0,3.0.0)</version>
</dependency>
<!-- WebSocket -->
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.4</version>
</dependency>
<!-- fastjson2 -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.26</version>
</dependency>
<!-- hutool -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.21</version>
</dependency>
<!-- protobuf -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.24.3</version>
</dependency>
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java-util</artifactId>
    <version>3.24.3</version>
</dependency>
<!-- slf4j-api -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
<!-- jmdns -->
<dependency>
    <groupId>org.jmdns</groupId>
    <artifactId>jmdns</artifactId>
    <version>3.5.8</version>
</dependency>

<!-- Non-mandatory -->
<!-- logback -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.3.11</version>
</dependency>
<!-- junit -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.8.1</version>
    <scope>test</scope>
</dependency>
```

### 3. Example
Here's an example purchase transaction.

```java
import com.wiseasy.ecr.hub.sdk.ECRHubClient;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.ECRHubClientFactory;
import com.wiseasy.ecr.hub.sdk.model.request.PurchaseRequest;
import com.wiseasy.ecr.hub.sdk.model.response.PurchaseResponse;

// 1、Create a client instance By Serial port
ECRHubConfig config = new ECRHubConfig();

// Method 1: Specify the serial port name. Please replace "xxxxxx" with the real serial port name. For example: COM6
// ECRHubClient client = ECRHubClientFactory.create("sp://xxxxxx", config);

// Method 2: Do not specify the serial port name. The SDK will automatically find available serial port
ECRHubClient client = ECRHubClientFactory.create("sp://", config);

// 2、Connecting to the server
client.connect();

// 3、Build PurchaseRequest
PurchaseRequest request = new PurchaseRequest();
request.setApp_id("Your payment appid"); // Setting your payment application ID
request.setMerchant_order_no("O123456789");
request.setOrder_amount("1");
request.setPay_method_category("BANKCARD");
// Setting read timeout,the timeout set here is valid for this request
// ECRHubConfig requestConfig = new ECRHubConfig();
// requestConfig.getSerialPortConfig().setReadTimeout(5 * 60 * 1000);
// request.setConfig(requestConfig);
        
// 4、Execute purchase request
PurchaseResponse response = client.execute(request);
System.out.println("Purchase Response:" + response);

// 5、Close connect
client.disconnect();
```
