package com.pmart5a.servaces;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageDesigner {

    public static String getSystemDesign(String message) {
        return String.format("[%s] %s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                message);
    }
}