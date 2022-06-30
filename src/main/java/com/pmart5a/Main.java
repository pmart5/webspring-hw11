package com.pmart5a;

import com.pmart5a.server.Server;

public class Main {

    private static final int PORT = 24321;
    private static final int N_THREADS = 64;

    public static void main(String[] args) {
        final var server = new Server(PORT, N_THREADS);
        server.start();
    }
}