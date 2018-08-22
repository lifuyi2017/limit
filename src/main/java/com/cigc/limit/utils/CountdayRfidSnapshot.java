//package com.cigc.limit.utils;
//
//import com.cigc.analysis.utils.DateUtils;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.client.transport.TransportClient;
//import org.elasticsearch.index.query.BoolQueryBuilder;
//import org.elasticsearch.index.query.PrefixQueryBuilder;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.index.query.RangeQueryBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
///**
// * Created by dc-dev on 2018/6/13.
// * 统计每天的数据源 rfid，抓拍，融合，异常数据
// */
//@Component
//public class CountdayRfidSnapshot {
//
//    @Autowired
//    private TransportClient client;
//    @Autowired
//    private JdbcTemplate source_jdbcTemplate;
//
//public void countAll(){
//long rfid= queryday("collectTime","RFID");
//long snapshot=queryday("passTime","SNAPSHOT");
//long afterVehicle=queryday("passTime","AfterVehicle");
//long invalidSnapshot=queryday("passTime","INVALIDSNAPSHOT");
//Long invalidRfid=queryday("collectTime","INVALIDRFID");
//String sql="INSERT INTO analysis.day_count_datasource  VALUES ("+rfid+","+snapshot+","+afterVehicle+","+invalidSnapshot+","+invalidRfid+",'"+DateUtils.getDayStr(-1)+"')";
//    source_jdbcTemplate.execute(sql);
//}
//
//public long  queryday(String field,String typename){
//    BoolQueryBuilder yuRootQuery = QueryBuilders.boolQuery();
//    RangeQueryBuilder startQuery = QueryBuilders.rangeQuery(field).gte(DateUtils.getMillis(-1,true));
//    RangeQueryBuilder endQuery = QueryBuilders.rangeQuery(field).lt(DateUtils.getMillis(0,true));
//    yuRootQuery.must(startQuery);
//    yuRootQuery.must(endQuery);
//
//    SearchResponse response = client.prepareSearch("cqct_20180508_*")
//            .setTypes(typename)
//            .setQuery(yuRootQuery)
//            .execute()
//            .actionGet();
//        System.out.println(response.getHits().getTotalHits());
//
//        return response.getHits().getTotalHits();
//
//}
//}
