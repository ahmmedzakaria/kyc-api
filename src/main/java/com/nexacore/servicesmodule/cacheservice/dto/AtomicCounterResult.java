package com.nexacore.servicesmodule.cacheservice.dto;

import java.time.Duration;

public record AtomicCounterResult(long value, Duration timeToLive) {
}
