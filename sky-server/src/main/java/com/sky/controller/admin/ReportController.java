package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;

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


    @ApiOperation("用户统计")
    @GetMapping("userStatistics")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate begin,
                                                   @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate end) {
        log.info("用户统计");
        UserReportVO userReportVO=reportService.userStatistics(begin,end);
        return Result.success(userReportVO);
    }


    @ApiOperation("订单统计")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                          LocalDate begin,
                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                          LocalDate end) {
        log.info("订单统计");
        OrderReportVO orderReportVO=reportService.ordersStatistics(begin,end);
        return Result.success(orderReportVO);
    }

    @ApiOperation("top10")
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> top10(@DateTimeFormat(pattern = "yyyy-MM-dd")
                                                LocalDate begin,
                                            @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                LocalDate end) {
        log.info("top10");
        SalesTop10ReportVO salesTop10ReportVO=reportService.top10(begin,end);
        return Result.success(salesTop10ReportVO);
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @GetMapping("/export")
    @ApiOperation("导出运营数据报表")
    public void export(HttpServletResponse response){
        reportService.exportBusinessData(response);
    }
}
