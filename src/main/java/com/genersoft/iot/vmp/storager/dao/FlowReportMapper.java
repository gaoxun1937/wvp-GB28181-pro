package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.FlowReport;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @Date 2022-09-27 10:26
 * @Created by gaoxu
 */
@Mapper
@Repository
public interface FlowReportMapper {
    @Insert("INSERT INTO flow_report (" +
            "app, " +
            "duration, " +
            "params, " +
            "player, " +
            "schemaName, " +
            "stream," +
            "totalBytes," +
            "vhost," +
            "ip," +
            "port," +
            "tcpId," +
            "mediaServiceId," +
            "createDate" +
            ") VALUES (" +
            "#{app}," +
            "#{duration}," +
            "#{params}," +
            "#{player}," +
            "#{schemaName}," +
            "#{stream}," +
            "#{totalBytes}," +
            "#{vhost}," +
            "#{ip}," +
            "#{port}," +
            "#{tcpId}," +
            "#{mediaServiceId}," +
            "#{createDate}" +
            ")")
    int add(FlowReport flowReport);
}
