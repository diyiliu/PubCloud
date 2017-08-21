package com.tiza.process.common.protocol.m2;

import cn.com.tiza.tstar.common.process.RPTuple;
import com.tiza.process.common.bean.*;
import com.tiza.process.common.cache.ICache;
import com.tiza.process.common.config.Constant;
import com.tiza.process.common.handler.M2ParseHandler;
import com.tiza.process.common.protocol.IDataProcess;
import com.tiza.process.common.util.CommonUtil;
import com.tiza.process.common.util.DateUtil;
import com.tiza.process.common.util.JacksonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description: M2DataProcess
 * Author: DIYILIU
 * Update: 2017-08-03 19:15
 */

@Service
public class M2DataProcess implements IDataProcess {
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected int cmd = 0xFF;

    protected static M2ParseHandler m2Handler;

    @Resource
    protected ICache m2CMDCacheProvider;

    @Resource
    protected ICache vehicleCacheProvider;

    @Override
    public Header dealHeader(byte[] bytes) {

        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        int length = buf.readUnsignedShort();

        byte[] termi = new byte[5];
        buf.readBytes(termi);
        String terminalId = CommonUtil.parseSIM(termi);

        int version = buf.readByte();

        int factory = buf.readByte();

        int terminalType = buf.readByte();

        int user = buf.readByte();

        int serial = buf.readUnsignedShort();

        int cmd = buf.readUnsignedByte();

        byte[] content = new byte[buf.readableBytes() - 3];
        buf.readBytes(content);

        int check = buf.readByte();

        byte[] end = new byte[2];
        buf.readBytes(end);

        return new M2Header(cmd, length, terminalId,
                version, factory, terminalType,
                user, serial, content,
                check, end);
    }

    @Override
    public void parse(byte[] content, Header header) {

    }

    public void init(){
        m2CMDCacheProvider.put(cmd, this);
    }


    public void toKafka(M2Header header, Position position, Status status){
        String terminalId = header.getTerminalId();
        if (!vehicleCacheProvider.containsKey(terminalId)) {
            logger.warn("该终端[{}]不存在车辆列表中...", terminalId);
            return;
        }

        VehicleInfo vehicle = (VehicleInfo) vehicleCacheProvider.get(terminalId);

        Map posMap = new HashMap() ;
        posMap.put(Constant.Location.GPS_TIME,
                DateUtil.dateToString(position.getDateTime()));
        posMap.put(Constant.Location.SPEED, position.getSpeed());
        posMap.put(Constant.Location.ALTITUDE, position.getHeight());
        posMap.put(Constant.Location.DIRECTION, position.getDirection());
        posMap.put(Constant.Location.LOCATION_STATUS, status.getLocation());
        posMap.put(Constant.Location.ACC_STATUS, status.getAcc());
        posMap.put(Constant.Location.ORIGINAL_LNG, position.getLngD());
        posMap.put(Constant.Location.ORIGINAL_LAT, position.getLatD());
        posMap.put(Constant.Location.LNG, position.getEnLngD());
        posMap.put(Constant.Location.LAT, position.getLatD());

        posMap.put("VehicleId", vehicle.getId());

        RPTuple rpTuple = new RPTuple();
        rpTuple.setCmdID(header.getCmd());
        rpTuple.setCmdSerialNo(header.getSerial());

        rpTuple.setTerminalID(String.valueOf(vehicle.getId()));

        String msgBody = JacksonUtil.toJson(posMap);
        rpTuple.setMsgBody(msgBody.getBytes(Charset.forName("UTF-8")));
        rpTuple.setTime(position.getDateTime().getTime());

        // 将解析的位置和状态信息放入流中
        RPTuple tuple = (RPTuple) header.gettStarData();
        tuple.setTerminalID(String.valueOf(vehicle.getId()));

        Map<String, String> context = tuple.getContext();

        context.put(Constant.FlowKey.POSITION, JacksonUtil.toJson(position));
        context.put(Constant.FlowKey.STATUS, JacksonUtil.toJson(status));


        logger.info("终端[{}]写入Kafka位置信息...", terminalId);
        m2Handler.storeInKafka(rpTuple, context.get(Constant.Kafka.TRACK_TOPIC));
    }

    public void toKafka(M2Header header, Position position, Status status, Parameter parameter) {
        String terminalId = header.getTerminalId();
        if (!vehicleCacheProvider.containsKey(terminalId)) {
            logger.warn("该终端[{}]不存在车辆列表中...", terminalId);
            return;
        }

        VehicleInfo vehicle = (VehicleInfo) vehicleCacheProvider.get(terminalId);

        Map posMap = new HashMap() ;
        posMap.put(Constant.Location.GPS_TIME,
                DateUtil.dateToString(position.getDateTime()));
        posMap.put(Constant.Location.SPEED, position.getSpeed());
        posMap.put(Constant.Location.ALTITUDE, position.getHeight());
        posMap.put(Constant.Location.DIRECTION, position.getDirection());
        posMap.put(Constant.Location.LOCATION_STATUS, status.getLocation());
        posMap.put(Constant.Location.ACC_STATUS, status.getAcc());
        posMap.put(Constant.Location.ORIGINAL_LNG, position.getLngD());
        posMap.put(Constant.Location.ORIGINAL_LAT, position.getLatD());
        posMap.put(Constant.Location.LNG, position.getEnLngD());
        posMap.put(Constant.Location.LAT, position.getLatD());

        posMap.put(Constant.Location.ROTATE_DIRECTION, parameter.getRotateDirection());
        posMap.put(Constant.Location.ROTATE_SPEED, parameter.getRotateSpeed());
        posMap.put(Constant.Location.FUEL_VOLUME, parameter.getFuelVolume());

        posMap.put("VehicleId", vehicle.getId());

        RPTuple rpTuple = new RPTuple();
        rpTuple.setCmdID(header.getCmd());
        rpTuple.setCmdSerialNo(header.getSerial());

        rpTuple.setTerminalID(String.valueOf(vehicle.getId()));

        String msgBody = JacksonUtil.toJson(posMap);
        rpTuple.setMsgBody(msgBody.getBytes(Charset.forName("UTF-8")));
        rpTuple.setTime(position.getDateTime().getTime());

        // 将解析的位置和状态信息放入流中
        RPTuple tuple = (RPTuple) header.gettStarData();
        tuple.setTerminalID(String.valueOf(vehicle.getId()));

        Map<String, String> context = tuple.getContext();
        context.put(Constant.FlowKey.POSITION, JacksonUtil.toJson(position));
        context.put(Constant.FlowKey.STATUS, JacksonUtil.toJson(status));
        context.put(Constant.FlowKey.PARAMETER, JacksonUtil.toJson(parameter));

        logger.info("终端[{}]写入Kafka位置信息...", terminalId);
        m2Handler.storeInKafka(rpTuple, context.get(Constant.Kafka.TRACK_TOPIC));
    }

    protected Position renderPosition(byte[] bytes) {

        if (bytes.length < 21) {
            logger.error("长度不足，无法获取位置信息！");
            return null;
        }

        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        long lat = buf.readUnsignedInt();
        long lng = buf.readUnsignedInt();
        int speed = buf.readUnsignedByte();
        int direction = buf.readUnsignedByte();
        byte[] heightBytes = new byte[2];
        buf.readBytes(heightBytes);
        int height = CommonUtil.renderHeight(heightBytes);
        byte[] statusBytes = new byte[4];
        buf.readBytes(statusBytes);
        long status = CommonUtil.bytesToLong(statusBytes);

        Date dateTime;
        byte[] dateBytes = null;
        if (bytes.length == 19) {
            dateBytes = new byte[3];
        } else if (bytes.length == 22) {
            dateBytes = new byte[6];
        }
        buf.readBytes(dateBytes);
        dateTime = CommonUtil.bytesToDate(dateBytes);

        return new Position(lng, lat, speed, direction, height, status, dateTime);
    }

    protected Status renderStatu(long l) {
        Status status = new Status();

        status.setLocation(statusBit(l, 0));
        status.setLat(statusBit(l, 1));
        status.setLng(statusBit(l, 2));
        status.setAcc(statusBit(l, 3));
        status.setLock(statusBit(l, 4));
        status.setDiscontinue(statusBit(l, 8));
        status.setPowerOff(statusBit(l, 9));
        status.setLowPower(statusBit(l, 10));
        status.setChangeSIM(statusBit(l, 11));
        status.setGpsFault(statusBit(l, 12));
        status.setLoseAntenna(statusBit(l, 13));
        status.setAerialCircuit(statusBit(l, 14));
        status.setPowerDefence(statusBit(l, 15));
        status.setOverSpeed(statusBit(l, 16));
        status.setTrailer(statusBit(l, 17));
        status.setUncap(statusBit(l, 18));

        return status;
    }
    private int statusBit(long l, int offset) {

        return new Long((l >> offset) & 0x01).intValue();
    }

    public static void setHandle(M2ParseHandler handle){
         m2Handler = handle;
    }
}
