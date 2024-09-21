package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dataList = new ArrayList<>();
        while (!startDate.equals(endDate)) {
            dataList.add(startDate);
            startDate = startDate.plusDays(1);
        }
        dataList.add(endDate);

        List<Double> turnoverList = new ArrayList<>();
        if(dataList!=null&&dataList.size()>0){
            for(LocalDate data : dataList){
                LocalDateTime begin=LocalDateTime.of(data, LocalTime.MIN);
                LocalDateTime end=LocalDateTime.of(data, LocalTime.MAX);
                log.info("营业额时间：{}，{}",begin,end);
                Integer status=Orders.COMPLETED;
                Map map=new HashMap();
                map.put("status",status);
                map.put("begin",begin);
                map.put("end",end);
                Double turnover=orderMapper.sumByMap(map);
                turnover= turnover==null?0.0:turnover;
                turnoverList.add(turnover);
            }
        }
        return TurnoverReportVO.builder().dateList(StringUtils.join(dataList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }
}
