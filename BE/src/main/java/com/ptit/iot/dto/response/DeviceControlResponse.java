package com.ptit.iot.dto.response;

import java.util.List;

public record DeviceControlResponse(String command, List<ActionLogDTO> actions) {
}
