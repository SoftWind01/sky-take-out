package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Api("营业额")
@Slf4j
@RequestMapping("/admin/report")
public class ReportController {
    @Autowired
    ReportService reportService;

    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                           LocalDate begin,
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate end) {
        log.info("营业额统计");
        TurnoverReportVO turnoverReportVO= reportService.turnoverStatistics(begin,end);
        return Result.success(turnoverReportVO);
    }
}
