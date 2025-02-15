package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.WatchService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    WorkspaceService workspaceService;

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

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dataList = new ArrayList<>();
        while (!begin.equals(end)) {
            dataList.add(begin);
            begin = begin.plusDays(1);
        }
        dataList.add(end);

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        if(dataList!=null&&dataList.size()>0){
            for(LocalDate data : dataList){
                LocalDateTime beginTime=LocalDateTime.of(data, LocalTime.MIN);
                LocalDateTime endTime=LocalDateTime.of(data, LocalTime.MAX);
                Integer newUser=userMapper.sumByTime(beginTime,endTime);
                Integer totalUser=userMapper.sumByTime(null,endTime);
                newUserList.add(newUser);
                totalUserList.add(totalUser);
            }
        }

        return UserReportVO.builder().
                dateList(StringUtils.join(dataList,",")).
                newUserList(StringUtils.join(newUserList,",")).
                totalUserList(StringUtils.join(totalUserList,",")).build();
    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dataList = new ArrayList<>();
        while (!begin.equals(end)) {
            dataList.add(begin);
            begin = begin.plusDays(1);
        }
        dataList.add(end);

        List<Integer> validOrderList = new ArrayList<>();
        List<Integer> totalOrderList = new ArrayList<>();
        if(dataList!=null&&dataList.size()>0){
            for(LocalDate data : dataList){
                LocalDateTime beginTime=LocalDateTime.of(data, LocalTime.MIN);
                LocalDateTime endTime=LocalDateTime.of(data, LocalTime.MAX);
                Map map=new HashMap();
                map.put("begin",beginTime);
                map.put("end",endTime);
                Integer totalOrders=orderMapper.countByMap(map);
                map.put("status",Orders.COMPLETED);
                Integer validOrders=orderMapper.countByMap(map);
                validOrderList.add(validOrders);
                totalOrderList.add(totalOrders);
            }
        }
        Integer total=totalOrderList.stream().reduce(Integer::sum).get();
        Integer valid=validOrderList.stream().reduce(Integer::sum).get();
        Double orderCompletionCountRate=0.0;
        if(total!=null&&total>0){
            orderCompletionCountRate=(double)valid/total;
        }
        return OrderReportVO.builder()
                .validOrderCount(valid)
                .validOrderCountList(StringUtils.join(validOrderList,","))
                .totalOrderCount(total)
                .orderCountList(StringUtils.join(totalOrderList,","))
                .orderCompletionRate(orderCompletionCountRate)
                .dateList(StringUtils.join(dataList,","))
                .build();
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin,LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()),",");
        String numberList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()),",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }


    /**导出近30天的运营数据报表
     * @param response
     **/
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //查询概览运营数据，提供给Excel模板文件
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于提供好的模板文件创建一个新的Excel表格对象
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //获得Excel文件中的一个Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            //获得第4行
            XSSFRow row = sheet.getRow(3);
            //获取单元格
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //准备明细数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            //通过输出流将文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.flush();
            out.close();
            excel.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
