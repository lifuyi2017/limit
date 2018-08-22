//package com.cigc.limit.utils;
//
//import com.cigc.analysis.utils.AppCfgUtils;
//import com.cigc.analysis.utils.DateUtils;
//import com.cigc.analysis.utils.FileUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.client.transport.TransportClient;
//import org.elasticsearch.index.query.BoolQueryBuilder;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.index.query.RangeQueryBuilder;
//import org.elasticsearch.search.aggregations.AggregationBuilders;
//import org.elasticsearch.search.aggregations.bucket.terms.Terms;
//import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created by sofn
// * 2018/5/9 9:42
// * es采集点和抓拍设备未录入点位分析
// */
//@Component
//public class NoPointInfoService {
//    private Log logger = LogFactory.getLog(this.getClass());
//    @Autowired
//    private JdbcTemplate source_jdbcTemplate;
//    @Autowired
//    private TransportClient client;
//
//    private int days = Integer.parseInt(AppCfgUtils.get("nopoint.miss.days"));
//
//    public  void queryRfidZp(){
//        zp();
//        rfid();
//    }
//
//    public void zp(){
//
//        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
//        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getMillis(-1, true));
//        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getMillis(0, true));
//        rootQuery.must(startQuery);
//        rootQuery.must(endQuery);
//        TermsAggregationBuilder termsAgg = AggregationBuilders.terms("snapCode").field("tollgateCode").size(Integer.MAX_VALUE);
//
//        SearchResponse response = client.prepareSearch(DateUtils.getIndexName())
//                .setTypes("SNAPSHOT")
//                .setQuery(rootQuery)
//                .addAggregation(termsAgg)
//                .execute()
//                .actionGet();
//
//        Terms snapTerms = response.getAggregations().get("snapCode");
//        HashSet<String> set = new HashSet<>();
//        for (Terms.Bucket bucket : snapTerms.getBuckets()) {
//            String tollgateCode = bucket.getKeyAsString();
//            List<Map<String, Object>> maps = source_jdbcTemplate.queryForList("SELECT FX from HC_ZS_STATIC_CJD_ZP where DEVICE_CODE='" + tollgateCode + "' ");
//            if ( maps.isEmpty()){
//                set.add(tollgateCode);
//            }
//        }
//        for (String s : set) {
//            //FileUtils.write("D:\\analNoInfo\\noInfo_snap_"+DateUtils.getDayStr(-1*days)+".txt",s);
//            source_jdbcTemplate.execute("INSERT  into analysis.day_count_nozp VALUE ('"+s+"','"+DateUtils.getDayStr(-1)+"')");
//        }
//    }
//
//    public void rfid(){
//        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
//        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("collectTime").from(DateUtils.getMillis(-1, true));
//        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("collectTime").to(DateUtils.getMillis(0, true));
//        rootQuery.must(startQuery);
//        rootQuery.must(endQuery);
//        TermsAggregationBuilder termsAgg = AggregationBuilders.terms("reader").field("readerIP").size(Integer.MAX_VALUE);
//
//        SearchResponse response = client.prepareSearch(DateUtils.getIndexName())
//                .setTypes("RFID")
//                .setQuery(rootQuery)
//                .addAggregation(termsAgg)
//                .execute()
//                .actionGet();
//
//        Terms snapTerms = response.getAggregations().get("reader");
//        HashSet<String> set = new HashSet<>();
//        for (Terms.Bucket bucket : snapTerms.getBuckets()) {
//            String readerIP = bucket.getKeyAsString();
//            List<Map<String, Object>> maps = source_jdbcTemplate.queryForList("SELECT FX from HC_ZS_STATIC_CJD_RFID where EQUIPMENT_RFID='" + readerIP + "' ");
//            if ( maps.isEmpty()){
//                set.add(readerIP);
//            }
//        }
//        for (String s : set) {
//            FileUtils.write("D:\\analNoInfo\\noInfo_rfid_"+DateUtils.getDayStr(-1*days)+".txt",s);
//            source_jdbcTemplate.execute("INSERT  into analysis.day_count_norfid VALUE ('"+s+"','"+DateUtils.getDayStr(-1)+"')");
//
//        }
//    }
//}
