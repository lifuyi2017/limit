package com.cigc.limit.service;

import com.cigc.limit.utils.AppCfgUtils;
import com.cigc.limit.utils.DateUtils;
import com.cigc.limit.utils.FileUtils;
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
 * Created by Administrator on 2018/7/9 0009.
 */
@Component
public class DeckService2 {
    @Autowired
    private TransportClient client;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    //双模点
    private static List<String> CJDID_List = new ArrayList<String>();

    public void searchData() {


        String sql = "select distinct(zp.CJDID) FROM  HC_ZS_STATIC_CJD_RFID rfid INNER JOIN  HC_ZS_STATIC_CJD_ZP zp on rfid.CJDID=zp.CJDID";
        CJDID_List = jdbcTemplate.queryForList(sql, String.class);

        Map<String,String>  finallyMap=new HashMap<>();

        Map<String, String> threeDayData1 = getThreeDayData(0);
        try {
            Thread.sleep(40000);
            System.out.println("线程睡眠40秒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map<String, String> threeDayData2 = getThreeDayData(-1);
        try {
            Thread.sleep(40000);
            System.out.println("线程睡眠40秒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map<String, String> threeDayData3 = getThreeDayData(-2);
        try {
            Thread.sleep(40000);
            System.out.println("线程睡眠40秒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map<String, String> threeDayData4 = getThreeDayData(-3);
        try {
            Thread.sleep(40000);
            System.out.println("线程睡眠40秒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map<String, String> threeDayData5 = getThreeDayData(-4);


        finallyMap.putAll(threeDayData1);
        finallyMap.putAll(threeDayData2);
        finallyMap.putAll(threeDayData3);
        finallyMap.putAll(threeDayData4);
        finallyMap.putAll(threeDayData5);

        System.out.println(finallyMap.size()+"===============");
        for(Map.Entry<String,String> entry:finallyMap.entrySet()){
            FileUtils.write("D:\\"+ AppCfgUtils.get("file"), entry.getKey() + "\t" + entry.getValue());
        }
    }

    private Map<String,String> getThreeDayData(int day) {
        Map<String,String> outMap=new HashMap<>();
        if(day==0){
            Map<String, String> threeDayOnlyRfidData = getThreeDayOnlyRfidData(day);
            try {
                Thread.sleep(40000);
                System.out.println("线程睡眠40秒");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outMap = getFilterData(-7, -3, threeDayOnlyRfidData);
        }else if(day==-4){
            Map<String, String> threeDayOnlyRfidData = getThreeDayOnlyRfidData(day);
            try {
                Thread.sleep(40000);
                System.out.println("线程睡眠40秒");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outMap = getFilterData(-4, -0, threeDayOnlyRfidData);
        }else {
            Map<String, String> threeDayOnlyRfidData = getThreeDayOnlyRfidData(day);
            try {
                Thread.sleep(40000);
                System.out.println("线程睡眠40秒");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outMap=getFilterData(day, 0, threeDayOnlyRfidData);
            Map<String,String> filterData2=getFilterData(-7, -3+day, threeDayOnlyRfidData);
            outMap.putAll(filterData2);
        }
        return outMap;
    }

    private Map<String, String> getFilterData(int start, int end, Map<String, String> threeDayOnlyRfidData) {

        List<String> codeList = new ArrayList<>();
        for (Map.Entry<String, String> entry : threeDayOnlyRfidData.entrySet()) {
            codeList.add(entry.getKey());
        }


        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getMillis(start, true));
        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getMillis(end, true));
        TermsQueryBuilder termsQuery = QueryBuilders.termsQuery("plateCode", codeList);
        TermsQueryBuilder termsQuery1 = QueryBuilders.termsQuery("cjdid", CJDID_List);

        rootQuery.filter(termsQuery);

        rootQuery.must(startQuery).must(endQuery).must(QueryBuilders.existsQuery("readerIP"))
                .must(QueryBuilders.existsQuery("tollgateCode")).must(termsQuery1);

        AggregationBuilder groupTerms = AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                .subAggregation(AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE));


        SearchResponse response = client.prepareSearch("cqct_20180508_*")
                .setTypes("AfterVehicle")
                .setQuery(rootQuery)
                .addAggregation(groupTerms)
                .setExplain(true).execute().actionGet();

        String code;
        String color;
        Map<String, String> codeMap = new HashMap<>();
        Terms groupCode = response.getAggregations().get("groupCode");
        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
            code = codeBucket.getKeyAsString();
            Terms groupColor = codeBucket.getAggregations().get("groupColor");
            for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                color = colorBucket.getKeyAsString();
                codeMap.put(code, color);
            }
        }
        System.out.println(codeMap.size()+"--------------");

        Map<String,String> outMap=new HashMap<>();
        for(Map.Entry<String, String> entry : threeDayOnlyRfidData.entrySet()){
            if (codeMap.containsKey(entry.getKey()) && codeMap.get(entry.getKey()).equals(entry.getValue())){
               outMap.put(entry.getKey(),entry.getValue());
            }
        }

        return outMap;

    }

    private Map<String, String> getThreeDayOnlyRfidData(int endtime) {
        Map<String, String> oneDayData = getOneDayData(endtime, endtime - 1);

        try {
            Thread.sleep(40000);
            System.out.println("线程睡眠40秒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, String> secondDayData = getOneDayData(endtime - 1, endtime - 2);

        Map<String, String> outMap = new HashMap<>();
        for (Map.Entry<String, String> entry : oneDayData.entrySet()) {
            if (secondDayData.containsKey(entry.getKey())) {
                if (secondDayData.get(entry.getKey()).equals(entry.getValue())) {
                    outMap.put(entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            Thread.sleep(40000);
            System.out.println("线程睡眠40秒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, String> threeDayData = getOneDayData(endtime - 2, endtime - 3);

        Map<String, String> out1Map = new HashMap<>();
        for (Map.Entry<String, String> entry : outMap.entrySet()) {
            if (threeDayData.containsKey(entry.getKey())) {
                if (threeDayData.get(entry.getKey()).equals(entry.getValue())) {
                    out1Map.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return out1Map;
    }


    private Map<String, String> getOneDayData(int endtime, int starttime) {

        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
        ExistsQueryBuilder exit1 = QueryBuilders.existsQuery("readerIP");
        ExistsQueryBuilder exit2 = QueryBuilders.existsQuery("snapsotId");
        ExistsQueryBuilder exit3 = QueryBuilders.existsQuery("tollgateCode");

        TermsQueryBuilder termsQuery = QueryBuilders.termsQuery("cjdid", CJDID_List);
        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getMillis(starttime, true));
        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getMillis(endtime, true));
        AggregationBuilder groupTerms = AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                .subAggregation(AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE));


        rootQuery.mustNot(exit2).must(exit1).must(termsQuery).must(startQuery).must(endQuery);


        SearchResponse response = client.prepareSearch("cqct_20180508_*")
                .setTypes("AfterVehicle")
                .setQuery(rootQuery)
                .addAggregation(groupTerms)
                .setExplain(true).execute().actionGet();


        Map<String, String> codeMap1 = new HashMap<>();



        String code;
        String color;
        Terms groupCode = response.getAggregations().get("groupCode");
        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
            code = codeBucket.getKeyAsString();
            Terms groupColor = codeBucket.getAggregations().get("groupColor");
            for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                color = colorBucket.getKeyAsString();
                codeMap1.put(code, color);
            }
        }
        System.out.println(codeMap1.size());



        BoolQueryBuilder rootQuery2 = QueryBuilders.boolQuery();
        TermsQueryBuilder termsQuery2 = QueryBuilders.termsQuery("plateCode", codeMap1.keySet());

        rootQuery2.filter(termsQuery2);

        rootQuery2.must(exit3).must(startQuery).must(endQuery).must(termsQuery);
        SearchResponse response2 = client.prepareSearch("cqct_20180508_*")
                .setTypes("AfterVehicle")
                .setQuery(rootQuery2)
                .addAggregation(groupTerms)
                .setExplain(true).execute().actionGet();

        Map<String, String> filterMap = new HashMap<>();

        Terms groupCode1 = response2.getAggregations().get("groupCode");
        for (Terms.Bucket codeBucket : groupCode1.getBuckets()) {
            code = codeBucket.getKeyAsString();
            Terms groupColor = codeBucket.getAggregations().get("groupColor");
            for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                color = colorBucket.getKeyAsString();
                filterMap.put(code, color);
            }
        }
        System.out.println(filterMap.size());



        Map<String, String> finallyMap = new HashMap<>();
        for (Map.Entry<String, String> entry : codeMap1.entrySet()) {
            if (!filterMap.containsKey(entry.getKey())) {
                finallyMap.put(entry.getKey(), entry.getValue());
            } else {
                if (!filterMap.get(entry.getKey()).equals(entry.getValue())) {
                    finallyMap.put(entry.getKey(), entry.getValue());
                }
            }


        }
        System.out.println(finallyMap.size());

        /*for (Map.Entry<String, String> entry : finallyMap.entrySet()) {
//            System.out.println(entry.getKey() + "---------" + entry.getValue());
            FileUtils.write("D:\\"+AppCfgUtils.get("file"), entry.getKey() + "\t" + entry.getValue());
        }*/

        return finallyMap;
    }


}
