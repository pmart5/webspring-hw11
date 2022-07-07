package com.pmart5a.server;

import com.pmart5a.interfaces.Handler;
import com.pmart5a.handlers.HandlerConnection;
import com.pmart5a.logger.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static com.pmart5a.enums.ErrorMsg.*;
import static com.pmart5a.enums.ParametersInt.*;
import static com.pmart5a.enums.SystemMsg.*;
import static com.pmart5a.servaces.MessageDesigner.getSystemDesign;

public class Server {

    public static final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server() {
    }

    public void start(int port, int nThreads) {
        Logger logger = Logger.getLogger();
        final var threadPool = Executors.newFixedThreadPool(nThreads);
        try (final var serverSocket = new ServerSocket(port)) {
            logger.logFileOut(getSystemDesign(String.format(SERVER_START.getMsg(), serverSocket.getLocalPort())));
            while (true) {
                final var socket = serverSocket.accept();
                final var handlerConnection = new HandlerConnection(socket);
                threadPool.submit(handlerConnection);
                logger.logFileOut(getSystemDesign(String.format(CLIENT_CONNECTED.getMsg(),
                        socket.getRemoteSocketAddress())));
            }
        } catch (IOException e) {
            logger.logFileOut(getSystemDesign(ERROR_INPUT_OUTPUT.getMsg() + e));
        } finally {
            threadPool.shutdown();
            logger.logFileOut(getSystemDesign(SERVER_STOP.getMsg()));
            logger.close();
        }
    }

    public void start(int port) {
        start(port, N_TREADS_DEFAULT.getValue());
    }

    public void start() {
        start(PORT_DEFAULT.getValue(), N_TREADS_DEFAULT.getValue());
    }

    public void addHandler(String method, String path, Handler handler) {
        if (handlers.containsKey(method)) {
            handlers.get(method).put(path, handler);
        } else {
            handlers.put(method, new ConcurrentHashMap<>(Map.of(path, handler)));
        }
    }
}