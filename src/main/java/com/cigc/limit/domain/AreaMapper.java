package com.cigc.limit.domain;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Administrator on 2018/7/3 0003.
 */
public class AreaMapper implements RowMapper<Area> {
    @Nullable
    @Override
    public Area mapRow(ResultSet rs, int rowNum) throws SQLException {
        Area area=new Area();
        area.setFX(rs.getString(1));
        area.setWeizhi(rs.getString(2));
        return area;
    }
}
