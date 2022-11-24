package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForServerStarted;
import com.genersoft.iot.vmp.media.zlm.dto.HookType;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;
import com.genersoft.iot.vmp.service.redisMsg.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.SIPRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * SIP命令类型： ACK请求
 */
@Component
public class AckRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

	private Logger logger = LoggerFactory.getLogger(AckRequestProcessor.class);
	private final String method = "ACK";

	@Autowired
	private SIPProcessorObserver sipProcessorObserver;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 添加消息处理的订阅
		sipProcessorObserver.addRequestProcessor(method, this);
	}

	@Autowired
    private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private ZlmHttpHookSubscribe hookSubscribe;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private ZlmHttpHookSubscribe subscribe;

	@Autowired
	private DynamicTask dynamicTask;

	@Autowired
	private ISIPCommander cmder;

	@Autowired
	private ISIPCommanderForPlatform commanderForPlatform;

	@Autowired
	private RedisGbPlayMsgListener redisGbPlayMsgListener;
	@Autowired
	private SipLayer sipLayer;
	@Autowired
	private SIPSender sipSender;
//    @Autowired
//    @Qualifier(value = "tcpSipProvider")
//    private SipProviderImpl tcpSipProvider;
//
//	@Autowired
//    @Qualifier(value = "udpSipProvider")
//    private SipProviderImpl udpSipProvider;

    @Autowired
	SIPRequestHeaderProvider requestHeaderProvider;
	/**
	 * 处理  ACK请求
	 *
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
//		Dialog dialog = evt.getDialog();
//		if (dialog == null) {
//			return;
//		}
		CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);

		String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
		logger.info("[收到ACK]： platformGbId->{}", platformGbId);
		ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(platformGbId);
		if(parentPlatform!=null) {
			// 取消设置的超时任务
			dynamicTask.stop(callIdHeader.getCallId());
			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(platformGbId, channelId, null, callIdHeader.getCallId());
			if (sendRtpItem == null) {
				logger.warn("[收到ACK]：未找到通道({})的推流信息", channelId);
				return;
			}
			String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
			MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
			logger.info("收到ACK，rtp/{}开始向上级推流, 目标={}:{}，SSRC={}", sendRtpItem.getStreamId(), sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc());
			Map<String, Object> param = new HashMap<>(12);
			param.put("vhost", "__defaultVhost__");
			param.put("app", sendRtpItem.getApp());
			param.put("stream", sendRtpItem.getStreamId());
			param.put("ssrc", sendRtpItem.getSsrc());
			param.put("dst_url", sendRtpItem.getIp());
			param.put("dst_port", sendRtpItem.getPort());
			param.put("is_udp", is_Udp);
			param.put("src_port", sendRtpItem.getLocalPort());
			param.put("pt", sendRtpItem.getPt());
			param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
			param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
			if (!sendRtpItem.isTcp() && parentPlatform.isRtcp()) {
				// 开启rtcp保活
				param.put("udp_rtcp_timeout", "1");
			}

			if (mediaInfo == null) {
				RequestPushStreamMsg requestPushStreamMsg = RequestPushStreamMsg.getInstance(
						sendRtpItem.getMediaServerId(), sendRtpItem.getApp(), sendRtpItem.getStreamId(),
						sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isTcp(),
						sendRtpItem.getLocalPort(), sendRtpItem.getPt(), sendRtpItem.isUsePs(), sendRtpItem.isOnlyAudio());
				redisGbPlayMsgListener.sendMsgForStartSendRtpStream(sendRtpItem.getServerId(), requestPushStreamMsg, jsonObject -> {
					startSendRtpStreamHand(evt, sendRtpItem, parentPlatform, jsonObject, param, callIdHeader);
				});
			} else {
				// 如果是非严格模式，需要关闭端口占用
				JSONObject startSendRtpStreamResult = null;
				if (sendRtpItem.getLocalPort() != 0) {
					if (zlmrtpServerFactory.releasePort(mediaInfo, sendRtpItem.getSsrc())) {
						startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
					}
				} else {
					startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
				}
				if (startSendRtpStreamResult != null) {
					startSendRtpStreamHand(evt, sendRtpItem, parentPlatform, startSendRtpStreamResult, param, callIdHeader);
				}
			}
		}else{
			//不是平台发送，则为语音包
			//增加订阅推流停止后发送bye包
			SendRtpItem sendRtpItem = redisCatchStorage.queryPushRTPServer(platformGbId, null, null, callIdHeader.getCallId());
			if (sendRtpItem != null) {
				MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
				//在这里开启推流
				Map<String, Object> param = new HashMap<>();
				JSONObject jsonObject;
				if (!sendRtpItem.isTcp()) {
					logger.info("UDP推流");
					//UDP推流
					param.put("vhost", "__defaultVhost__");
					param.put("app", "broadcast");
					param.put("stream", sendRtpItem.getStreamId());
					param.put("ssrc", sendRtpItem.getSsrc());
					param.put("dst_url", sendRtpItem.getIp());
					param.put("dst_port", sendRtpItem.getPort());
					param.put("is_udp", "1");
					param.put("pt", sendRtpItem.getPt());
					param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
					param.put("src_port", sendRtpItem.getLocalPort());
					param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
					jsonObject = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
				} else {
					logger.info("TCP推流");
					//TCP推流
					param.put("vhost", "__defaultVhost__");
					param.put("app", "broadcast");
					param.put("stream", sendRtpItem.getStreamId());
					param.put("ssrc", sendRtpItem.getSsrc());
//						String pt = sdp.getAttribute("rtpmap");
					param.put("pt", sendRtpItem.getPt());
					param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
					param.put("src_port", sendRtpItem.getLocalPort());
					param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
					jsonObject = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
				}
				logger.info("开始推语音流 rtp/{}", param);
				if (jsonObject == null) {
					logger.error("RTP推流失败: 请检查ZLM服务");
				} else if (jsonObject.getInteger("code") == 0) {
					logger.info("RTP推流成功[ {}/{} ]，{}->{}:{}, ", param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
//					int localPort = Integer.parseInt(jsonObject.getString("local_port"));
//					JSONObject hookJson = new JSONObject();
//					hookJson.put("app", "broadcast");
//					hookJson.put("stream", sendRtpItem.getStreamId());
					HookSubscribeForServerStarted broadcast = HookSubscribeFactory.on_broadcast("broadcast", sendRtpItem.getStreamId());
					subscribe.addSubscribe(broadcast, (MediaServerItem mediaServerItem, JSONObject response) -> {
						try {
							Request byeRequest = null;
							//请求行
							SIPRequest sipRequest = (SIPRequest)evt.getRequest();
							SipURI requestLine = (SipURI)sipRequest.getRequestURI();
							// via
							ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
							ViaHeader viaHeader = sipRequest.getTopmostViaHeader();
							viaHeaders.add(viaHeader);
							//from
							FromHeader fromHeader = sipRequest.getFromHeader();
							//to
							ToHeader toHeader = sipRequest.getToHeader();
							//Forwards
							MaxForwardsHeader maxForwards = sipLayer.getSipFactory().createHeaderFactory().createMaxForwardsHeader(70);

							//ceq
							CSeqHeader cSeqHeader = sipRequest.getCSeqHeader();
							byeRequest = sipLayer.getSipFactory().createMessageFactory().createRequest(requestLine, Request.BYE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

							byeRequest.addHeader(sipRequest.getContactHeader());
							viaHeader = (ViaHeader) evt.getRequest().getHeader(ViaHeader.NAME);
							String received = viaHeader.getReceived();
							int rPort = viaHeader.getRPort();
							if (!StringUtils.isEmpty(received) && rPort != -1) {
								SipURI byeURI = (SipURI) byeRequest.getRequestURI();
								byeURI.setHost(received);
								byeURI.setPort(rPort);
							}
							String protocol = viaHeader.getTransport().toUpperCase();
							redisCatchStorage.deletePushRTPServer(sendRtpItem.getPlatformId(), sendRtpItem.getChannelId(), sendRtpItem.getCallId(), sendRtpItem.getStreamId());
							if ("TCP".equals(protocol)) {
								sipLayer.getTcpSipProvider().sendRequest(byeRequest);
							} else if ("UDP".equals(protocol)) {
								sipLayer.getUdpSipProvider().sendRequest(byeRequest);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				}
			}
		}
	}
	private void startSendRtpStreamHand(RequestEvent evt, SendRtpItem sendRtpItem, ParentPlatform parentPlatform,
										JSONObject jsonObject, Map<String, Object> param, CallIdHeader callIdHeader) {
		if (jsonObject == null) {
			logger.error("RTP推流失败: 请检查ZLM服务");
		} else if (jsonObject.getInteger("code") == 0) {
			logger.info("调用ZLM推流接口, 结果： {}",  jsonObject);
			logger.info("RTP推流成功[ {}/{} ]，{}->{}:{}, " ,param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
		} else {
			logger.error("RTP推流失败: {}, 参数：{}",jsonObject.getString("msg"), JSON.toJSONString(param));
			if (sendRtpItem.isOnlyAudio()) {
				// TODO 可能是语音对讲
			}else {
				// 向上级平台
				try {
					commanderForPlatform.streamByeCmd(parentPlatform, callIdHeader.getCallId());
				} catch (SipException | InvalidArgumentException | ParseException e) {
					logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
				}
			}
		}
	}
}
