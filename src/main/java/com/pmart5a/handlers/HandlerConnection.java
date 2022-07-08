package com.pmart5a.handlers;

import com.pmart5a.logger.Logger;
import com.pmart5a.request.Request;
import org.apache.hc.core5.net.URIBuilder;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.*;

import static com.pmart5a.Main.sendError;
import static com.pmart5a.enums.ErrorMsg.*;
import static com.pmart5a.enums.ParametersInt.*;
import static com.pmart5a.enums.ParametersStrings.*;
import static com.pmart5a.enums.ResponseMsg.*;
import static com.pmart5a.enums.SystemMsg.*;
import static com.pmart5a.server.Server.*;

import static com.pmart5a.services.MessageFormatter.getSystemDesign;

public class HandlerConnection implements Runnable {

    private final Socket socket;
    private final BufferedInputStream in;
    private final BufferedOutputStream out;

    public HandlerConnection(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        Logger logger = Logger.getLogger();
        try {
            final var allowedMethods = List.of(METHOD_GET.getValue(), METHOD_POST.getValue());
            final var limit = BUFFER_LIMIT.getValue();
            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                sendError(RESPONSE_400.getMsg(), out);
                return;
            }

            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                sendError(RESPONSE_400.getMsg(), out);
                return;
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                sendError(RESPONSE_400.getMsg(), out);
                return;
            }
            logger.logFileOut(method);

            final var pathAndQuery = requestLine[1];
            if (!pathAndQuery.startsWith("/")) {
                sendError(RESPONSE_404.getMsg(), out);
                return;
            }

            final var sep = pathAndQuery.indexOf("?");
            String path = pathAndQuery;
            if (sep != -1) {
                path = pathAndQuery.substring(0, sep);
            }
            logger.logFileOut(path);

            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                sendError(RESPONSE_400.getMsg(), out);
                return;
            }

            in.reset();
            in.skip(headersStart);
            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headersList = Arrays.asList(new String(headersBytes).split("\r\n"));
            Map<String, String> headers = new HashMap<>();
            for (String header : headersList) {
                var separator = header.indexOf(":");
                var name = header.substring(0, separator);
                var value = header.substring(separator + 2);
                headers.put(name, value);
                logger.logFileOut(header);
            }

            String body = "";
            if (!method.equals(METHOD_GET.getValue())) {
                in.skip(headersDelimiter.length);
                final var contentLength = extractHeader(headersList, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);
                    body = new String(bodyBytes);
                    logger.logFileOut(body);
                }
            }

            final var params = new URIBuilder(pathAndQuery).getQueryParams();
            logger.logFileOut(String.valueOf(params));

            final var request = new Request(method, path, params, headers, body);
            logger.logFileOut(request.toString());

            final var handler = handlers.get(request.getMethod()).get(request.getPath());
            handler.handle(request, out);

        } catch (IOException | URISyntaxException e) {
            logger.logFileOut(getSystemDesign(ERROR_INPUT_OUTPUT.getMsg() + e));
        } finally {
            try {
                logger.logFileOut(getSystemDesign(String.format(CLIENT_DISCONNECTED.getMsg(),
                        socket.getRemoteSocketAddress())));
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                logger.logFileOut(getSystemDesign(ERROR_INPUT_OUTPUT.getMsg() + e));
            }
        }
    }

    private Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}