package com.ptit.iot.service;

import com.ptit.iot.dto.response.HistoryRecordDTO;
import com.ptit.iot.dto.response.PageResponse;

public interface HistoryService {
	PageResponse<HistoryRecordDTO> search(int page, int pageSize, String deviceFilter, String actionFilter,
			String statusFilter, String performedBy, String from, String to, String sortBy, String sortDir);
}
