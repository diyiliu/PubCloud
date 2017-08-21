package com.tiza.process.common.dao.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Description: BaseDao
 * Author: DIYILIU
 * Update: 2017-08-07 16:55
 */

public class BaseDao {

    private static Logger logger = LoggerFactory.getLogger(BaseDao.class);

    protected static JdbcTemplate jdbcTemplate;

    public static void  initDataSource(DataSource dataSource){
        if (jdbcTemplate == null){
            logger.info("装载数据源...");
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }

    public static JdbcTemplate getJdbcTemplate(){

        return jdbcTemplate;
    }
}
