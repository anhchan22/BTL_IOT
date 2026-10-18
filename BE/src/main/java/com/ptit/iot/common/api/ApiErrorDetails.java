package com.ptit.iot.common.api;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorDetails(OffsetDateTime timestamp, int status, String path, Map<String, String> details) {
}
