package com.cigc.limit.service;


import com.cigc.limit.utils.DateUtils;
import com.cigc.limit.utils.FileUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 大渡口区政府附近采集点每小时，车流统计
 */
@Component
public class DaDuKouService {
    @Autowired
    private TransportClient client;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void anal() {
        HashMap<String, String> map = new HashMap<>();
        map.put("ZCQ10130", "文体路（区府） 钢花路方向,29.483383143246,106.480656301791");
        map.put("ZCQ10104", "翠园路（松青路口） 钢花路方向,29.480501148209,106.48039913144");
        map.put("ZCQ10103", "翠园路（松青路口） 春晖路方向,29.480486387129,106.480206888162");
        map.put("ZCQ10105", "翠园路（松青路口） 榕花街方向,29.480581013092,106.480180040456");
        map.put("ZCQ10099", "春晖路大渡口轻轨站 文体路方向,29.482210186354,106.478418958656");
        map.put("ZCQ10098", "春晖路大渡口轻轨站 翠园路方向,29.482181980908,106.478142110835");
        map.put("ZCQ10131", "文体路（区府） 文体立交方向,29.48346244806,106.481318754659");
        map.put("ZCQ10116", "钢花路（育才小学） 文体路方向,29.487438778277,106.487702354901");
        map.put("ZCQ10109", "钢花路（八桥街路口） 翠园路方向,29.475466165151,106.484044639716");
        map.put("ZCQ10111", "钢花路（八桥街路口） 锦霞街方向,29.475337753872,106.484175103726");
        map.put("ZCQ10110", "钢花路（八桥街路口） 钢花路方向,29.475476449137,106.483986845564");

        long starttime = 1529251200000l; //4.27 0点
        long step = 60 * 60 * 1000;
        for (int i = 1; i <= 168; i++) {
//            //客车
            String keche[] = new String[]{"113", "117", "121"};
            es(map, starttime + (i - 1) * step, starttime + i * step, keche, "客车");
//            //牵引车
            String qiangua[] = new String[]{"130", "131", "132", "133", "135"};
            es(map, starttime + (i - 1) * step, starttime + i * step, qiangua, "牵引车");
//            //专项作业车
            String zuoye[] = new String[]{"140", "141", "142", "143", "144", "145"};
            es(map, starttime + (i - 1) * step, starttime + i * step, zuoye, "专项作业车");
            //货车
            String huoche[] = new String[]{"60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97"};
            HashMap<String, Long> huo = es(map, starttime + (i - 1) * step, starttime + i * step, huoche, "货车");
//            for (Map.Entry<String, Long> entry : huo.entrySet()) {
////                String key = entry.getKey();
////                String s = map.get(key.split(",")[0]);
////                String plateCode = key.split(",")[1];
////                String plateColor = key.split(",")[2];
////                Long value = entry.getValue();
////                FileUtils.write("D:\\货车.txt",s+","+plateCode+","+plateColor+","+value+","+sdf.format(new Date(starttime + i * step)));
////            }
//            //挂车
            String guache[] = new String[]{"34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59"};
            es(map, starttime + (i - 1) * step, starttime + i * step, guache, "挂车");
//            //半挂车
            String banguache[] = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"};
            es(map, starttime + (i - 1) * step, starttime + i * step, banguache, "半挂车");
        }
    }

    public HashMap<String, Long> es(HashMap<String, String> map, long starttime, long endtime, String[] type, String carType) {
        HashMap<String, Long> map1 = new HashMap<>();
        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
        TermsQueryBuilder termsQuery = QueryBuilders.termsQuery("cjdid", map.keySet());
        TermsQueryBuilder termsQuery2 = QueryBuilders.termsQuery("epcVehicleCode", type);
        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").gt(starttime);
        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").lte(endtime);
        rootQuery.must(termsQuery);
        rootQuery.must(termsQuery2);
        rootQuery.must(startQuery);
        rootQuery.must(endQuery);
        AggregationBuilder plateTerms =
                AggregationBuilders
                        .terms("groupCjd").field("cjdid").size(Integer.MAX_VALUE);
                        /*.subAggregation(
                                AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                                        .subAggregation(
                                                AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE)
                                        ));*/

        SearchResponse response = client.prepareSearch(DateUtils.getIndexNameByTime(sdf.format(new Date(starttime))))
                .setTypes("AfterVehicle")
                .setQuery(rootQuery)
                .addAggregation(plateTerms)
                .execute()
                .actionGet();
        String info = null;
        Terms groupCjd = response.getAggregations().get("groupCjd");
        for (Terms.Bucket cjdBucket : groupCjd.getBuckets()) {
            String cjd = cjdBucket.getKeyAsString();
            /*Terms groupCode = cjdBucket.getAggregations().get("groupCode");
            for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
                String plateCode = codeBucket.getKeyAsString();
                Terms groupColor = codeBucket.getAggregations().get("groupColor");
                for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                    String plateColor = colorBucket.getKeyAsString();
                    String key = cjd+","+plateCode+","+plateColor;
                    long docCount = colorBucket.getDocCount();
                    map1.put(key,docCount);
                    *//*Integer count = map1.get(key);
                    if (count == null) {
                        map1.put(key, 1);
                    } else map1.put(key, count + 1);*//*
                }
            }*/


            long count = cjdBucket.getDocCount();
            info = carType + "," + count + "," + map.get(cjd) + "," + sdf.format(new Date(endtime));
//            System.out.println("数据:"+info);
            FileUtils.write("D:\\anal.txt", info);
        }
        return map1;
    }
}
