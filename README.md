# ECRHubClient SDK for Java

## About this sdk
Help Java developers quickly use ECRHubClient SDK to achieve LAN cross-device service calls. Developers do not need to care about the complexity of the internal logic , just call a method to complete a functional operation .

## Features
- **Multiple connection mode:** This SDK provides multiple ways to connect to Cashier, including serial port and websocket. You can choose the applicable connection method according to your ECR system environment.
- **Payments type supported:** This SDK provides a variety of payment methods, including bank card code scanning, and also provides a lot of intermediary payment capabilities, such as consumption, revocation, refund, pre-authorization, pre-authorization completion and so on.

## Getting Started

### 1. Requirements
- Suitable for Java language, JDK version 1.8 and above development environment.

### 2. Steps
- Download the jar package and add it to your project, please refer to github source code. <a href = "https://github.com/paycloud-open/ecrhub-client-sdk-java" target = "_blank">ecrhub-client-sdk-java</a>
- This JAR depends on some open source third party JARs. If these JARs are not integrated in your project, you will need to manually add dependencies to your project.

```XML
<!-- mandatory -->
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

<!-- non-mandatory -->
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

// Create a client instance By Serial port

// Setting up your payment application ID
ECRHubConfig config = new ECRHubConfig("Your payment appid");
// Please replace "xxxxxx" with the real serial port name
ECRHubClient client = ECRHubClientFactory.create("sp://xxxxxx", config);
// Connecting to the server
client.connect();

// Build PurchaseRequest
PurchaseRequest request = new PurchaseRequest();
request.setMerchant_order_no("O123456789");
request.setOrder_amount("1");
request.setPay_method_category("BANKCARD");

// Execute purchase request
PurchaseResponse response = client.execute(request);
System.out.println("Purchase Response:" + response);

// Close connect
client.disconnect();
```