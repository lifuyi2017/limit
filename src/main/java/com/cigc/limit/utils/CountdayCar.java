//package com.cigc.limit.utils;
//
//import com.cigc.analysis.utils.DateUtils;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.client.transport.TransportClient;
//import org.elasticsearch.index.query.*;
//import org.elasticsearch.search.aggregations.AggregationBuilder;
//import org.elasticsearch.search.aggregations.AggregationBuilders;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
///**
// * Created by dc-dev on 2018/6/14.
// * 统计每一天的公交车，出租车，小轿车，货运车，其他车
// */
//@Component
//public class CountdayCar {
//    @Autowired
//    private TransportClient client;
//    @Autowired
//    private JdbcTemplate source_jdbcTemplate;
//
//    public void countAllCar(){
//       long bus=queryday("epcNatureCode","3");
//       long taxi=queryday("epcNatureCode","4");
//       long sedan=queryday("epcVehicleCode","117");
//       long freight=queryday("epcNatureCode","6");
//       long other=allCar()-bus-taxi-sedan-freight;
//       String sql="insert into analysis.day_count_car value ("+bus+","+taxi+","+sedan+","+freight+","+other+",'"+DateUtils.getDayStr(-1)+"')";
//       source_jdbcTemplate.execute(sql);
//    }
//
//
//
//    public long  queryday(String filed,String type){
//        BoolQueryBuilder yuRootQuery = QueryBuilders.boolQuery();
//        TermsQueryBuilder bigTermsQuery = QueryBuilders.termsQuery(filed, type);
//        yuRootQuery.must(bigTermsQuery);
//        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").gte(DateUtils.getMillis(-1,true));
//        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").lt(DateUtils.getMillis(0,true));
//
//        yuRootQuery.must(startQuery);
//        yuRootQuery.must(endQuery);
//
//        AggregationBuilder plateTerms =
//                AggregationBuilders
//                        .terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
//                        ;
//
//
//        SearchResponse response = client.prepareSearch("cqct_20180508_*")
//                .setTypes("AfterVehicle")
//                .setQuery(yuRootQuery)
//                .addAggregation(plateTerms)
//                .execute()
//                .actionGet();
//
//        //System.out.println(response.getHits().getTotalHits());
//        Terms groupCode = response.getAggregations().get("groupCode");
//        long amount = 0l;
//        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
//                amount++;
//        }
//        return amount;
//
//
//    }
//
//    public long  allCar(){
//        BoolQueryBuilder yuRootQuery = QueryBuilders.boolQuery();
//        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").gte(DateUtils.getMillis(-1,true));
//        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").lt(DateUtils.getMillis(0,true));
//
//        yuRootQuery.must(startQuery);
//        yuRootQuery.must(endQuery);
//
//        AggregationBuilder plateTerms =
//                AggregationBuilders
//                        .terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
//                ;
//
//
//        SearchResponse response = client.prepareSearch("cqct_20180508_*")
//                .setTypes("AfterVehicle")
//                .setQuery(yuRootQuery)
//                .addAggregation(plateTerms)
//                .execute()
//                .actionGet();
//
//        //System.out.println(response.getHits().getTotalHits());
//        Terms groupCode = response.getAggregations().get("groupCode");
//        long amount = 0l;
//        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
//            amount++;
//        }
//        return amount;
//    }
//}
