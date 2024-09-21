package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO turnoverStatistics(LocalDate startDate, LocalDate endDate);

    UserReportVO userStatistics(LocalDate begin, LocalDate end);
}
