package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.ECRHubTerminal;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketClientListener;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketServerEngine;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 09:40
 */
public class ECRHubSocketServer extends ECRHubAbstractServer implements WebSocketClientListener {

    private static final Logger log = LoggerFactory.getLogger(ECRHubSocketServer.class);

    private final WebSocketServerEngine engine;

    private JmDNS jmDNS;

    private final ConcurrentMap<WebSocket, ECRHubTerminal> hubTerminalConcurrentMap = new ConcurrentHashMap<>(1);

    private final Set<ECRHubTerminal> ecrHubTerminals = new ConcurrentHashSet<>(1);

    private ClientListener clientListener;


    public ECRHubSocketServer(ECRHubConfig config) {
        super(config);
        ECRHubConfig.SocketServerConfig socketServerConfig = config.getSocketServerConfig();
        int port = socketServerConfig.getPort();
        engine = new WebSocketServerEngine(port);
        engine.setClientListener(this);
    }


    @Override
    public void onOpen(WebSocket socket) {
        InetSocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
        ECRHubTerminal ecrHubTerminal = new ECRHubTerminal(null,
                null,
                remoteSocketAddress.getAddress().getHostAddress(),
                null, remoteSocketAddress.getPort(), socket);
        hubTerminalConcurrentMap.put(socket, ecrHubTerminal);
        ecrHubTerminals.add(ecrHubTerminal);
        if (null != clientListener) {
            clientListener.onConnect(ecrHubTerminal);
        }
    }

    @Override
    public void onClose(WebSocket socket, int code, String reason, boolean remote) {
        ECRHubTerminal ecrHubTerminal = hubTerminalConcurrentMap.get(socket);
        ecrHubTerminals.remove(ecrHubTerminal);
        if (null != clientListener) {
            clientListener.onDisconnect(ecrHubTerminal, code, reason, remote);
        }
    }

    @Override
    public boolean start() throws ECRHubException {
        engine.start();
        int timeout = config.getSocketConfig().getConnTimeout();
        long before = System.currentTimeMillis();
        while (!engine.isRunning()) {
            if (System.currentTimeMillis() - before > timeout) {
                throw new ECRHubException("service start error");
            } else {
                log.info("service starting... ");
                ThreadUtil.safeSleep(200);
            }
        }

        ECRHubConfig.SocketServerConfig socketServerConfig = config.getSocketServerConfig();

        boolean registerMDNS = socketServerConfig.isRegisterMDNS();
        if (registerMDNS) {
            try {
                InetAddress siteLocalAddress = NetHelper.getLocalhost();
                jmDNS = JmDNS.create(siteLocalAddress);
                JSONObject info = new JSONObject();
                info.put("mac_address", NetUtil.getMacAddress(siteLocalAddress));
                info.put("os_name", SystemUtil.getOsInfo().getName());
                info.put("os_version", SystemUtil.getOsInfo().getVersion());
                String localHostName = NetHelper.getLocalHostName();
                info.put("host_name", localHostName);
                ServiceInfo serviceInfo = ServiceInfo.create("_ECRHub-client._tcp.local.",
                        StrUtil.format("ECRHub-Client-{}", localHostName), engine.getPort(), info.toJSONString());
                jmDNS.registerService(serviceInfo);
            } catch (IOException e) {
                throw new ECRHubException(e);
            }
        }

        return true;
    }

    @Override
    public boolean stop() throws ECRHubException {
        try {
            engine.stop();
            if (null != jmDNS) {
                try {
                    jmDNS.close();
                } catch (IOException ignored) {

                }
            }
        } catch (InterruptedException e) {
            throw new ECRHubException(e);
        }
        return true;
    }

    @Override
    public void setClientListener(ClientListener clientStatusListener) {
        this.clientListener = clientStatusListener;
    }

    @Override
    public List<ECRHubTerminal> getTerminalList() {
        return new ArrayList<>(ecrHubTerminals);
    }

    @Override
    public boolean disconnect(ECRHubTerminal terminal) {
        terminal.getSocket().close(1, "The server actively closes the connection");
        return true;
    }

    @Override
    public <T extends ECRHubResponse> T execute(ECRHubTerminal terminal, ECRHubRequest<T> request) throws ECRHubException {
        sendReq(terminal, request);
        return getResp(request);
    }

    protected <T extends ECRHubResponse> void sendReq(ECRHubTerminal terminal, ECRHubRequest<T> request) throws ECRHubException {
        if (null == terminal) {
            throw new ECRHubException("Terminal must not be null");
        }
        if (null == terminal.getSocket() || !terminal.getSocket().isOpen()) {
            throw new ECRHubException("The terminal is closed and the device needs to be reconnected");
        }
        WebSocket socket = terminal.getSocket();
        byte[] msg = ECRHubProtobufHelper.pack(getConfig(), request);
        socket.send(new String(msg));
    }

    protected <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException {
        ECRHubConfig config = request.getConfig();
        long timeout = config != null ? config.getSocketServerConfig().getReceiveTimeOut() : DEF_READ_TIMEOUT;
        String msg = engine.receive(request.getMsg_id(), timeout);
        return decodeRespPack(msg.getBytes(StandardCharsets.UTF_8), request.getResponseClass());
    }

    @Override
    public <T extends ECRHubResponse> void asyncExecute(ECRHubTerminal terminal, ECRHubRequest<T> request, ECRHubResponseCallBack<T> callback) throws ECRHubException {
        sendReq(terminal, request);
        ThreadUtil.execute(() -> {
            try {
                callback.onResponse(getResp(request));
            } catch (ECRHubTimeoutException e) {
                callback.onTimeout(e);
            } catch (ECRHubException e) {
                callback.onError(e);
            } catch (Exception e) {
                callback.onError(new ECRHubException(e));
            }
        });

    }
}
