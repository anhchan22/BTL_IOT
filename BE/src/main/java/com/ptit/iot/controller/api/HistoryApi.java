package com.ptit.iot.controller.api;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.dto.response.HistoryRecordDTO;
import com.ptit.iot.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Device history")
@RequestMapping("/api/history")
public interface HistoryApi {
	@Operation(summary = "Tra cứu lịch sử điều khiển")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	ApiResponse<PageResponse<HistoryRecordDTO>> search(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(name = "page_size", defaultValue = "15") int pageSize,
			@RequestParam(required = false) String device,
			@RequestParam(name = "device_code", required = false) String deviceCode,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) String status,
			@RequestParam(name = "performed_by", required = false) String performedBy,
			@RequestParam(name = "username", required = false) String username,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(name = "sort_by", defaultValue = "time") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir);
}
