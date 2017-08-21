package com.tiza.process.common.handler;

import cn.com.tiza.tstar.common.datasource.BusinessDBManager;
import cn.com.tiza.tstar.common.process.BaseHandle;
import cn.com.tiza.tstar.common.process.RPTuple;
import cn.com.tiza.tstar.common.utils.DBUtil;
import com.tiza.process.common.config.Constant;
import com.tiza.process.common.protocol.m2.M2DataProcess;
import com.tiza.process.common.cache.ICache;
import com.tiza.process.common.util.CommonUtil;
import com.tiza.process.common.util.SpringUtil;
import com.tiza.process.common.bean.M2Header;
import com.tiza.process.common.dao.base.BaseDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.reflect.Field;

/**
 * Description: M2ParseHandler
 * Author: DIYILIU
 * Update: 2017-08-02 14:06
 */

public class M2ParseHandler extends BaseHandle {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public RPTuple handle(RPTuple rpTuple) throws Exception {
        logger.info("收到终端[{}], 指令[{}]...", rpTuple.getTerminalID(), CommonUtil.toHex(rpTuple.getCmdID()));

        ICache m2CMDCacheProvider = SpringUtil.getBean("m2CMDCacheProvider");
        M2DataProcess process = (M2DataProcess) m2CMDCacheProvider.get(rpTuple.getCmdID());
        if (process == null) {
            logger.error("无法找到[{}]指令解析器!", CommonUtil.toHex(rpTuple.getCmdID()));
            return null;
        }

        rpTuple.getContext().put(Constant.Kafka.TRACK_TOPIC, processorConf.get("trackTopic"));
        M2Header header = (M2Header) process.dealHeader(rpTuple.getMsgBody());
        header.settStarData(rpTuple);
        process.parse(header.getContent(), header);

        return rpTuple;
    }

    @Override
    public void init() throws Exception {
        M2DataProcess.setHandle(this);
        SpringUtil.init("m2-bean.xml");

        BusinessDBManager dbManager = BusinessDBManager.getInstance(this.processorConf);
        Field field = dbManager.getClass().getDeclaredField("dbUtil");
        field.setAccessible(true);
        DBUtil dbUtil = (DBUtil) field.get(dbManager);
        field.setAccessible(false);

        // 初始化数据源
        BaseDao.initDataSource(dbUtil.getDataSource());
    }
}
