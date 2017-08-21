package com.tiza.gateways.common;

import cn.com.tiza.tstar.common.entity.TStarData;
import cn.com.tiza.tstar.gateway.entity.AckData;
import cn.com.tiza.tstar.gateway.entity.CommandData;
import cn.com.tiza.tstar.gateway.handler.BaseUserDefinedHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description: M2ProtoHandler
 * Author: DIYILIU
 * Update: 2017-08-08 14:39
 */
public abstract class M2ProtoHandler extends BaseUserDefinedHandler {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String config = "";

    @Override
    public TStarData handleRecvMessage(ChannelHandlerContext context, ByteBuf byteBuf) {
        TStarData tStarData = new TStarData();

        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        tStarData.setMsgBody(bytes);
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        buf.readShort();
        byte[] terminalArray = new byte[5];
        buf.readBytes(terminalArray);

        String terminal = parseTerminal(terminalArray);
        tStarData.setTerminalID(terminal);

        buf.readInt();
        int serial = buf.readUnsignedShort();
        int cmd = buf.readUnsignedByte();

        tStarData.setCmdID(cmd);
        tStarData.setCmdSerialNo(serial);
        tStarData.setTime(System.currentTimeMillis());

        logger.info("收到消息，终端[{}]指令[{}], 内容[{}]...", terminal, String.format("%02X", cmd), bytesToStr(bytes));

        TStarData respData = new TStarData();
        respData.setTerminalID(terminal);
        respData.setTime(System.currentTimeMillis());
        if (0x80 == cmd) {
            Properties properties = initProperties();

            String apn = properties.getProperty("apn");
            String ip = properties.getProperty("ip");
            int port = Integer.parseInt(properties.getProperty("port"));

            byte[] apnBytes = apn.getBytes();

            ByteBuf respBuf = Unpooled.buffer(1 + apnBytes.length + 4 + 2);
            respBuf.writeByte(apnBytes.length);
            respBuf.writeBytes(apnBytes);
            respBuf.writeBytes(ipToBytes(ip));
            respBuf.writeShort(port);

            byte[] respMsg = createResp(bytes, respBuf.array(), 0x01);
            respData.setCmdID(0x01);
            respData.setMsgBody(respMsg);
            context.channel().writeAndFlush(respData);
        } else if (0x85 == cmd) {

            ByteBuf respBuf = Unpooled.buffer(4);
            respBuf.writeShort(serial);
            respBuf.writeByte(cmd);
            respBuf.writeByte(0);

            byte[] respMsg = createResp(bytes, respBuf.array(), 0x02);
            respData.setCmdID(0x02);
            respData.setMsgBody(respMsg);
            context.channel().writeAndFlush(respData);
        }

        return tStarData;
    }

    @Override
    public AckData ackHandle(ChannelHandlerContext ctx, TStarData msg) {
        AckData ackData = null;
        int cmd = msg.getCmdID();
        if (0x082 == cmd){
            byte[] bytes = msg.getMsgBody();
            ByteBuf buf = Unpooled.copiedBuffer(bytes, 14, bytes.length - 14);

            int serial = buf.readShort();
            int respCmd = buf.readByte();
            ackData = new AckData(msg, respCmd, serial);

            logger.info("响应指令[{}, {}], 流水号[{}]", String.format("%02X", cmd), String.format("%02X", respCmd), serial);
        }else if (0x83 == cmd) {
            int respCmd = 0x03;
            String terminalId = msg.getTerminalID();

            // sim卡后四位 + respCmd
            int serial = Integer.parseInt(terminalId.substring(terminalId.length() - 4)) + respCmd;
            ackData = new AckData(msg, respCmd, serial);

            logger.info("响应指令[{}, {}], 流水号[{}]", String.format("%02X", cmd), String.format("%02X", respCmd), serial);
        }else if (0x84 == cmd){
            byte[] bytes = msg.getMsgBody();
            ByteBuf buf = Unpooled.copiedBuffer(bytes, 14, bytes.length - 14);
            buf.readByte();
            int paramId = buf.readShort();

            int respCmd = 0x07;
            String terminalId = msg.getTerminalID();
            // sim卡后四位 + respCmd + 参数id
            int serial = Integer.parseInt(terminalId.substring(terminalId.length() - 4)) + respCmd + paramId;
            ackData = new AckData(msg, respCmd, serial);

            logger.info("响应指令[{}, {}], 流水号[{}]", String.format("%02X", cmd), String.format("%02X", respCmd), serial);
        }

        return ackData;
    }


    @Override
    public void commandReceived(ChannelHandlerContext ctx, CommandData cmd) {

        logger.info("下发消息，终端[{}]指令[{}], 内容[{}]...", cmd.getTerminalID(), String.format("%02X", cmd), bytesToStr(cmd.getMsgBody()));
    }

    /**
     * 生成回复指令内容
     *
     * @param recMsg
     * @param content
     * @return
     */
    public byte[] createResp(byte[] recMsg, byte[] content, int cmd) {

        int length = 14 + content.length;
        recMsg[0] = (byte) ((length >> 8) & 0xff);
        recMsg[1] = (byte) (length & 0xff);

        ByteBuf header = Unpooled.copiedBuffer(recMsg, 0, 11);

        ByteBuf remainder = Unpooled.buffer(3 + content.length + 3);
        remainder.writeShort(getMsgSerial());
        remainder.writeByte(cmd);
        remainder.writeBytes(content);

        byte check = getCheck(Unpooled.copiedBuffer(header.array(),
                Unpooled.copiedBuffer(remainder.array(), 0, 3 + content.length).array()).array());

        remainder.writeByte(check);
        remainder.writeByte(0x0D);
        remainder.writeByte(0x0A);

        return Unpooled.copiedBuffer(header, remainder).array();
    }

    /**
     * 解析终端ID
     *
     * @param bytes
     * @return
     */
    public String parseTerminal(byte[] bytes) {
        Long sim = 0l;
        int len = bytes.length;
        for (int i = 0; i < len; i++) {
            sim += (long) (bytes[i] & 0xff) << ((len - i - 1) * 8);
        }

        return sim.toString();
    }

    /**
     * IP封装字节数组
     *
     * @param host
     * @return
     */
    public byte[] ipToBytes(String host) {

        String[] array = host.split("\\.");

        byte[] bytes = new byte[array.length];

        for (int i = 0; i < array.length; i++) {

            bytes[i] = (byte) Integer.parseInt(array[i]);
        }

        return bytes;
    }

    /**
     * 命令序号
     **/
    private static AtomicLong msgSerial = new AtomicLong(0);

    private int getMsgSerial() {
        Long serial = msgSerial.incrementAndGet();
        if (serial > 65535) {
            msgSerial.set(0);
            serial = msgSerial.incrementAndGet();
        }

        return serial.intValue();
    }


    /**
     * 获取校验位
     *
     * @param bytes
     * @return
     */
    private byte getCheck(byte[] bytes) {
        byte b = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            b ^= bytes[i];
        }

        return b;
    }


    /**
     * 字节数组转字符串
     *
     * @param bytes
     * @return
     */
    public static String bytesToStr(byte[] bytes) {
        StringBuffer buf = new StringBuffer();
        for (byte a : bytes) {
            buf.append(String.format("%02X", getNoSin(a)));
        }

        return buf.toString();
    }

    /**
     * 无符号位字节转整数
     *
     * @param b
     * @return
     */
    public static int getNoSin(byte b) {
        if (b >= 0) {
            return b;
        } else {
            return 256 + b;
        }
    }

    public Properties initProperties(){
        setConfig();

        Properties properties = null;
        InputStream inputStream = null;
        try {
            properties = new Properties();
            inputStream = new ClassPathResource(config).getInputStream();
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return properties;
    }


    public abstract void setConfig();
}
