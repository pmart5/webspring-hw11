package com.pmart5a.interfaces;

import com.pmart5a.request.Request;

import java.io.BufferedOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface Handler {
    void handle(Request request, BufferedOutputStream responseStream) throws IOException;
}
