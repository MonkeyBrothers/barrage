package top.houry.netty.barrage.service.impl;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.houry.netty.barrage.annotation.BarrageAnnotation;
import top.houry.netty.barrage.consts.BarrageMsgTypeConst;
import top.houry.netty.barrage.consts.BarrageRedisKeyConst;
import top.houry.netty.barrage.consts.BarrageVideoConst;
import top.houry.netty.barrage.proto.BarrageProto;
import top.houry.netty.barrage.service.IBarrageMsgService;
import top.houry.netty.barrage.service.IBarrageMsgTypeService;
import top.houry.netty.barrage.service.IBarrageWatchInfoService;
import top.houry.netty.barrage.utils.BarrageConnectInfoUtils;
import top.houry.netty.barrage.utils.BarrageRedisUtils;

import java.util.List;

/**
 * @Desc
 * @Author houruiyang
 * @Date 2022/1/22
 **/
@Service
@BarrageAnnotation(msgType = BarrageMsgTypeConst.WEB_CLIENT_LOGIN_REQ)
@Slf4j
public class BarrageClientLoginMsgServiceImpl implements IBarrageMsgTypeService {

    private IBarrageMsgService msgService;

    private IBarrageWatchInfoService watchInfoService;

    @Autowired
    public void setBarrageWatchInfoService(IBarrageWatchInfoService barrageWatchInfoService) {
        this.watchInfoService = barrageWatchInfoService;
    }

    @Autowired
    public void setMsgService(IBarrageMsgService msgService) {
        this.msgService = msgService;
    }

    @Override
    public void dealWithBarrageMessage(BarrageProto.Barrage barrage, ChannelHandlerContext ctx) {
        try {
            BarrageProto.WebClientLoginReq loginInfo = BarrageProto.WebClientLoginReq.parseFrom(barrage.getBytesData());
            String userId = StringUtils.isBlank(loginInfo.getUserId()) ? "" : loginInfo.getUserId();
            String videoId = StringUtils.isBlank(loginInfo.getVideoId()) ? "" : loginInfo.getVideoId();
            log.info("[Req]-[BarrageClientLoginMsgServiceImpl]-[dealWithBarrageMessage]-[userId:{}]-[videoId:{}]", userId, videoId);
            BarrageConnectInfoUtils.addChannelInfoToBaseMap(videoId, ctx);

            cacheOnlinePopulationAndWatchCountToRedis(videoId);

            BarrageProto.Barrage.Builder builder = BarrageProto.Barrage.newBuilder();
            BarrageProto.WebClientLoginResp.Builder loginResp = BarrageProto.WebClientLoginResp.newBuilder();

            loginResp.setBarrageOnlineCount(Integer.parseInt(watchInfoService.getTotalOnlineWatchCount(videoId)));
            loginResp.setBarrageTotalCount(msgService.getMsgCountByVideoId(videoId));
            loginResp.setBarrageTotalWatchCount(Integer.parseInt(watchInfoService.getTotalWatchCount(videoId)));

            builder.setBytesData(loginResp.build().toByteString());
            builder.setMsgType(BarrageMsgTypeConst.WEB_CLIENT_LOGIN_RESP);

            ctx.writeAndFlush(builder);

        } catch (Exception e) {
            log.error("[Exception]-[BarrageClientLoginMsgServiceImpl]-[dealWithBarrageMessage]", e);
        }
    }

    private void cacheOnlinePopulationAndWatchCountToRedis(String videoId) {
        watchInfoService.addOnlineWatchCount(videoId);
        watchInfoService.addTotalWatchCount(videoId);
    }
}
