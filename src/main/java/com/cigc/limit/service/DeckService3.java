package com.cigc.limit.service;

import com.cigc.limit.utils.AppCfgUtils;
import com.cigc.limit.utils.DateUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/7/10.
 * 查询一天内只有抓拍而没有RFID的车牌，且这种情况出现了5次
 */
@Component
public class DeckService3 {

    @Autowired
    private TransportClient client;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    //双模点
    private static List<String> CJDID_List = new ArrayList<String>();

    public void searchData(){
        String sql = "select distinct(zp.CJDID) FROM  HC_ZS_STATIC_CJD_RFID rfid INNER JOIN  HC_ZS_STATIC_CJD_ZP zp on rfid.CJDID=zp.CJDID";
        CJDID_List = jdbcTemplate.queryForList(sql, String.class);

        //7天内有抓拍又有RFID
        BoolQueryBuilder DayQuery = QueryBuilders.boolQuery();
        ExistsQueryBuilder exitSnapshot = QueryBuilders.existsQuery("snapsotId");

        AggregationBuilder groupTerms = AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                .subAggregation(AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE));
        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getMillis(-AppCfgUtils.getInt("starttime"), true));
        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getMillis(-AppCfgUtils.getInt("endtime"), true));
        TermsQueryBuilder termsQuery = QueryBuilders.termsQuery("cjdid", CJDID_List);

        DayQuery.must(exitSnapshot).must(startQuery).must(endQuery).must(termsQuery).mustNot(QueryBuilders.existsQuery("readerIP"));
        SearchResponse response = client.prepareSearch("cqct_20180508_*")
                .setTypes("AfterVehicle")
                .setQuery(DayQuery)
                .addAggregation(groupTerms)
                .setExplain(true).execute().actionGet();

        String code;
        String color;
        Map<String,String> codeMap=new HashMap<>();
        Terms groupCode = response.getAggregations().get("groupCode");
        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
            code = codeBucket.getKeyAsString();
            Terms groupColor = codeBucket.getAggregations().get("groupColor");
            for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                color = colorBucket.getKeyAsString();
                codeMap.put(code, color);
            }
        }

        System.out.println(codeMap.size());





    }



}
