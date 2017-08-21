package com.tiza.process.common.dao;

import com.tiza.process.common.bean.VehicleInfo;
import com.tiza.process.common.config.Constant;
import com.tiza.process.common.dao.base.BaseDao;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Description: VehicleDao
 * Author: DIYILIU
 * Update: 2017-08-07 14:58
 */

@Component
public class VehicleDao extends BaseDao {

    public List<VehicleInfo> selectVehicleInfo() {
        String sql = Constant.getSQL(Constant.SQL.SELECT_VEHICLE_INFO);

        return jdbcTemplate.query(sql, new RowMapper<VehicleInfo>() {
            @Override
            public VehicleInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                VehicleInfo vehicleInfo = new VehicleInfo();
                vehicleInfo.setId(rs.getInt("id"));
                vehicleInfo.setTerminalId(rs.getLong("terminalid"));
                vehicleInfo.setDeviceId(rs.getString("deviceid"));
                vehicleInfo.setTerminalNo(rs.getString("terminalno"));
                vehicleInfo.setSim(rs.getString("simno"));
                vehicleInfo.setProtocolType(rs.getString("protocoltype"));

                return vehicleInfo;
            }
        });
    }


    public boolean update(String sql, Object[] values){
       int result =  jdbcTemplate.update(sql, values);

       if (result > 0){

           return true;
       }

       return false;
    }
}
