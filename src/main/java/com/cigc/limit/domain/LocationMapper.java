package com.cigc.limit.domain;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Administrator on 2018/7/2 0002.
 */
public class LocationMapper implements RowMapper<Location> {

    @Nullable
    @Override
    public Location mapRow(ResultSet resultSet, int i) throws SQLException {

            Location location=new Location();
            location.setLat(resultSet.getDouble(1));
            location.setLng(resultSet.getDouble(2));
            return location;

    }
}
