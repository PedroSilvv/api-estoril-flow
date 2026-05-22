package com.estorilflow.support;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class ApplicationClock {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("America/Sao_Paulo");

    private ApplicationClock() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(APPLICATION_ZONE);
    }
}
