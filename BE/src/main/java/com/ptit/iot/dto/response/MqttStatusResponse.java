package com.ptit.iot.dto.response;

import java.time.OffsetDateTime;

public record MqttStatusResponse(boolean connected, OffsetDateTime checkedAt) {
}
