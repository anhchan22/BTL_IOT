package com.ptit.iot.controller;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.common.api.ApiResponseFactory;
import com.ptit.iot.controller.api.HistoryApi;
import com.ptit.iot.dto.response.HistoryRecordDTO;
import com.ptit.iot.dto.response.PageResponse;
import com.ptit.iot.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HistoryController implements HistoryApi {
	private final HistoryService historyService;
	private final ApiResponseFactory responseFactory;

	@Override
	public ApiResponse<PageResponse<HistoryRecordDTO>> search(int page, int pageSize, String device, String deviceCode,
			String action, String status, String performedBy, String username, String from, String to,
			String sortBy, String sortDir) {
		return responseFactory.success(historyService.search(page, pageSize, device != null ? device : deviceCode,
				action, status, performedBy != null ? performedBy : username, from, to, sortBy, sortDir));
	}
}
