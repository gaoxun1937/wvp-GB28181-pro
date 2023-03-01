package com.genersoft.iot.vmp.gb28181.bean;

import java.util.Date;

/**
 * 流量统计
 *
 * @Date 2022-09-27 10:20
 * @Created by gaoxu
 */
public class FlowReport {
    private Long id;
    private String app;
    private Integer duration;
    private String params;
    private String player;
    private String schemaName;
    private String stream;
    private Long totalBytes;
    private String vhost;
    private String ip;
    private Integer port;
    private String tcpId;
    private String mediaServiceId;
    private Date createDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getTcpId() {
        return tcpId;
    }

    public void setTcpId(String tcpId) {
        this.tcpId = tcpId;
    }

    public String getMediaServiceId() {
        return mediaServiceId;
    }

    public void setMediaServiceId(String mediaServiceId) {
        this.mediaServiceId = mediaServiceId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
