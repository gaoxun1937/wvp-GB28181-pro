package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.FlowReport;
import com.genersoft.iot.vmp.service.IFlowReportService;
import com.genersoft.iot.vmp.storager.dao.FlowReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Date 2022-09-27 10:28
 * @Created by gaoxu
 */
@Service
public class FlowReportServiceImpl implements IFlowReportService {
    @Autowired
    private FlowReportMapper flowReportMapper;
    @Override
    public void add(FlowReport flowReport) {
        flowReportMapper.add(flowReport);
    }
}
