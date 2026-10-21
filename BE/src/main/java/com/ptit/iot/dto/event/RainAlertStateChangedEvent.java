package com.ptit.iot.dto.event;

import com.ptit.iot.dto.response.ActionLogDTO;
import com.ptit.iot.dto.response.DeviceDTO;
import com.ptit.iot.dto.response.RainAlertStatusResponse;

public record RainAlertStateChangedEvent(RainAlertStatusResponse status, ActionLogDTO actionLog, DeviceDTO device) { }
