package com.cigc.limit.service;


import com.cigc.limit.utils.DateUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.acl.LastOwnerException;
import java.util.*;

@Component
public class TaoPai {

    @Autowired
    private TransportClient client;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    //双模点
    private static List<String> CJDID_List = new ArrayList<String>();

    private static Long defTime = 60000L;

//    private static int startTime = 1;

//    private static int endTime = 0;


    public void searchAllData() {

        String sql = "select distinct(zp.CJDID) FROM  HC_ZS_STATIC_CJD_RFID rfid INNER JOIN  HC_ZS_STATIC_CJD_ZP zp on rfid.CJDID=zp.CJDID";
        CJDID_List = jdbcTemplate.queryForList(sql, String.class);
        System.out.println("-------------------------------------------");
        String index = "cqct_20180508_*";
        String type = "AfterVehicle";
        //建立查询条件
        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
        //过滤融合成功数据
        ExistsQueryBuilder sbuilder = QueryBuilders.existsQuery("tollgateCode");
        //过滤单模点
        TermsQueryBuilder termsQuery = QueryBuilders.termsQuery("cjdid", CJDID_List);
        //按车牌号和车牌颜色进行group by聚合，
        AggregationBuilder plateTerms =
                AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                        .subAggregation(AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE)
                                .subAggregation(AggregationBuilders.terms("groupCJDID").field("cjdid").size(Integer.MAX_VALUE)
                                        .subAggregation(AggregationBuilders.terms("groupTime").field("passTime").size(Integer.MAX_VALUE)))
                        );
        //查出一天数据
//        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getMillis(-startTime, true));
//        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getMillis(0, true));


        Date date=new Date();
        int hours=date.getHours();
        int miute=date.getMinutes();
        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getTimeStmap(-(hours*60+miute+1440*3+1440)));
        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getTimeStmap(-(hours*60+miute+1440*3+720)));

        rootQuery.mustNot(sbuilder);
        rootQuery.must(startQuery);
        rootQuery.must(endQuery);
        rootQuery.must(termsQuery);


        //setFrom,setSize设置分页
        SearchResponse response = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(rootQuery)
                .addAggregation(plateTerms)
                .setExplain(true).execute().actionGet();
        //聚合查询分桶统计,
        Tuple<String, String> tuple;
        String code = "";
        String color = "";
        String cjdId = "";
        Long pTime;
        TreeMap<Long, String> valueMap;
        TreeMap<Long, String> timeMap;
        //RFID数据，key为车牌ID+车牌颜色，value：（时间+地点ID）集合
        Map<String, TreeMap<Long, String>> rfidMap = new HashMap<>();


        Terms groupCode = response.getAggregations().get("groupCode");
        //车牌桶
        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
            valueMap = new TreeMap<>();
            Terms groupColor = codeBucket.getAggregations().get("groupColor");
            code = codeBucket.getKeyAsString();
            //颜色桶
            for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                Terms groupCjdId = colorBucket.getAggregations().get("groupCJDID");
                color = colorBucket.getKeyAsString();

                //cjdid桶
                for (Terms.Bucket cjdidBucket : groupCjdId.getBuckets()) {
                    Terms groupTime = cjdidBucket.getAggregations().get("groupTime");
                    cjdId = cjdidBucket.getKeyAsString();
                    //时间桶
                    for (Terms.Bucket timeBucket : groupTime.getBuckets()) {
                        pTime = (Long) timeBucket.getKey();
                        valueMap.put(pTime, cjdId);
                    }
                }
                rfidMap.put(code + "-" + color, valueMap);
            }
        }

        Map<String, TreeMap<Long, String>> finallyRfidMap = new HashMap<>();

        //筛选大于5次的
        Long old;
        for (Map.Entry<String, TreeMap<Long, String>> entry : rfidMap.entrySet()) {
            if (entry.getValue().size() > 5) {
                finallyRfidMap.put(entry.getKey(), entry.getValue());
            }
        }
        //筛选完成
        rfidMap.clear();
        System.out.println("++++++++++++++++++++" + finallyRfidMap.size());


        //查抓拍
        BoolQueryBuilder rootQuery2 = QueryBuilders.boolQuery();

        //过滤单模点
        TermsQueryBuilder termsQuery2 = QueryBuilders.termsQuery("cjdid", CJDID_List);
        //过滤
        ExistsQueryBuilder sbuilder2 = QueryBuilders.existsQuery("snapsotId");
        //按车牌号和车牌颜色进行group by聚合，
        AggregationBuilder plateTerms2 =
                AggregationBuilders.terms("groupCJDID").field("cjdid").size(Integer.MAX_VALUE)
                        .subAggregation(AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE)
                                .subAggregation(AggregationBuilders.terms("groupTime").field("passTime").size(Integer.MAX_VALUE)
                                        .subAggregation(AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)))
                        );
        rootQuery2.must(sbuilder2);
        rootQuery2.must(startQuery);
        rootQuery2.must(endQuery);
        rootQuery2.must(termsQuery2);
        SearchResponse response2 = client.prepareSearch("cqct_20180508_*").setTypes("ExceptionAfterVehicle")
                .setQuery(rootQuery2)
                .addAggregation(plateTerms2)
                .setExplain(true).execute().actionGet();

        String code2 = "";
        String color2 = "";
        String cjdId2 = "";
        Long pTime2;
        //抓拍数据输出格式，key：地点+车牌颜色，value：Map（时间：车牌号）
        TreeMap<String, TreeMap<Long, String>> snapsotMap = new TreeMap<>();

        String outKey;
        TreeMap<Long, String> valueTree;

        Terms groupCjdId2 = response2.getAggregations().get("groupCJDID");
        //颜色桶
        for (Terms.Bucket colorBucket : groupCjdId2.getBuckets()) {
            Terms groupColor2 = colorBucket.getAggregations().get("groupColor");
            cjdId2 = colorBucket.getKeyAsString();
            //cjdid桶
            for (Terms.Bucket cjdidBucket : groupColor2.getBuckets()) {
                Terms groupTime2 = cjdidBucket.getAggregations().get("groupTime");
                color2 = cjdidBucket.getKeyAsString();
                outKey = cjdId2 + "-" + color2;
                valueTree = new TreeMap<>();
                //时间桶
                for (Terms.Bucket timeBucket : groupTime2.getBuckets()) {
                    Terms groupCode2 = timeBucket.getAggregations().get("groupCode");
                    pTime2 = (Long) timeBucket.getKey();
                    //车牌桶
                    for (Terms.Bucket codeBucket : groupCode2.getBuckets()) {
                        code2 = codeBucket.getKeyAsString();
                        valueTree.put(pTime2, code2);

                    }
                }
                snapsotMap.put(outKey, valueTree);
            }
        }

        System.out.println(snapsotMap.size());

        /*for (Map.Entry<Tuple<String, String>, List<Long>> entry:snapsotMap.entrySet()){
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
*/


        //输出类型,key为rfid检测到的车牌号，value为可能的套牌车号，可能性由高到低
        Map<String, TreeMap<Integer, String>> output = new HashMap<>();
        //中间变量,key：RFID车牌号，value：通过点的抓拍数据车牌号
        Map<String, Map<String, Integer>> midleMap = new HashMap<>();
        Map<String, Integer> midleValueMap;
        Integer mideleInt;

        //数据验证
        //时间集合 TreeMap<Long, Tuple<String, String>> snapsotMap
        List<Long> timeList = new ArrayList<>();
        Long rfidTime;
        Long lastDefTime;
        //RFID数据，key为车牌ID+车牌颜色，value：（时间+地点ID）集合
        for (Map.Entry<String, TreeMap<Long, String>> entry : finallyRfidMap.entrySet()) {
            //连续位置
            for (Map.Entry<Long, String> entry0 : entry.getValue().entrySet()) {
                //抓拍数据输出格式，key：地点+车牌颜色，value：Map（时间：车牌号）
                for (Map.Entry<String, TreeMap<Long, String>> entry1 : snapsotMap.entrySet()) {
                    //相同点与相同车牌颜色
                    if (entry1.getKey().equals(entry0.getValue() + "-" + entry.getKey().substring(entry.getKey().indexOf("-") + 1))) {
                        //时间最接近
                        rfidTime = entry0.getKey();
                        lastDefTime = Long.valueOf(Integer.MAX_VALUE);
                        //
                        for (Map.Entry<Long, String> entry2 : entry1.getValue().entrySet()) {
                            if (Math.abs(entry2.getKey() - rfidTime) < lastDefTime) {
                                lastDefTime = Math.abs(entry2.getKey() - rfidTime);
                            } else {
                                //时间最近点
                                //时间范围条件要满足
                                if(lastDefTime<defTime){
                                    if (midleMap.containsKey(entry.getKey())) {
                                        midleValueMap = midleMap.get(entry.getKey());
                                    } else {
                                        midleValueMap = new HashMap<>();
                                    }

                                    if (midleValueMap.containsKey(entry2.getValue())) {
                                        mideleInt=midleValueMap.get(entry2.getValue());
                                        mideleInt+=1;
                                    }else {
                                        mideleInt=1;
                                    }
                                    midleValueMap.put(entry2.getValue(),mideleInt);
                                    midleMap.put(entry.getKey(), midleValueMap);

                               /* System.out.println(entry.getKey() + "==============================" );
                                //打印map
                                Iterator  itor = midleValueMap.keySet().iterator();
                                while(itor.hasNext())
                                {
                                    String key = (String)itor.next();
                                    String value = String.valueOf(midleValueMap.get(key));
                                    if(Integer.parseInt(value)>5){
                                        System.out.println(key+"=============="+value);
                                    }
                                }
                                System.out.println("======================================");
*/
                                break;
                                }
                            }
                        }



                    }


                }
            }
        }



        //过滤其中大于5条的
        //RFID车牌，车牌颜色，抓拍车牌，抓拍车牌颜色，次数
        Map<Tuple<String,String>,Tuple<Tuple<String,String>,Integer>>  outPutMap=new HashMap<>();
//        List<String> outCodeList=new ArrayList<>();
        for(Map.Entry<String, Map<String, Integer>> entry:midleMap.entrySet()){
            if( (entry.getValue().get(getMapMax(entry.getValue()))>5 || entry.getValue().get(getMapMax(entry.getValue()))==5)) {
                outPutMap.put(new Tuple<>(entry.getKey().substring(0,entry.getKey().indexOf("-")),entry.getKey().substring(entry.getKey().indexOf("-")+1)),
                        new Tuple<>(new Tuple<>(getMapMax(entry.getValue()),entry.getKey().substring(entry.getKey().indexOf("-")+1)),
                                entry.getValue().get(getMapMax(entry.getValue()))));
//                outCodeList.add(entry.getKey().substring(0,entry.getKey().indexOf("-")));
            }
        }
//        System.out.println(CJDID_List.size()+"-------------"+outCodeList.size());//421
/*
        //数据清洗
        //次数相同的太多一般是某一辆车停在某个采集点附近不动造成的,RFID数据CJD很多相同，得把这种数据过滤掉
        BoolQueryBuilder filterRoot = QueryBuilders.boolQuery();
        //时间点

        //双模
//        TermsQueryBuilder termsQuery3 = QueryBuilders.termsQuery("cjdid", CJDID_List);
        //车牌
        TermsQueryBuilder termsQueryCode = QueryBuilders.termsQuery("plateCode", outCodeList);
        //分组查询
        AggregationBuilder plateTerms3 =
                AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                        .subAggregation(AggregationBuilders.terms("groupCJDID").field("cjdid").size(Integer.MAX_VALUE));

        filterRoot.must(startQuery);
        filterRoot.must(endQuery);
//        filterRoot.must(termsQuery3);
        filterRoot.must(termsQueryCode);

        SearchResponse filterResponse = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(filterRoot)
                .addAggregation(plateTerms3)
                .setExplain(true).execute().actionGet();

        String code3="";
        String cjdid3="";

        //用于过滤的数据，key：RFID的车牌号，value：cjdid与出现次数
        Map<String,Map<String,Integer>> filterMap=new HashMap<>();
        Map<String,Integer> mildleFilterMap=new HashMap<>();
        Integer mildleFilteLong;
        Terms groupCode3 = filterResponse.getAggregations().get("groupCode");
        for (Terms.Bucket codeBucket : groupCode3.getBuckets()) {
            Terms groupCjdid3 = codeBucket.getAggregations().get("groupCJDID");
            code3 = codeBucket.getKeyAsString();
            System.out.println("--------------"+code3);
            if(filterMap.containsKey(code3)){
                mildleFilterMap=filterMap.get(code3);
            }else {
                mildleFilterMap=new HashMap<>();
            }
            for (Terms.Bucket cjdidBucket : groupCjdid3.getBuckets()) {
                cjdid3 = cjdidBucket.getKeyAsString();
                if (mildleFilterMap.containsKey(cjdid3)){
                    mildleFilteLong=mildleFilterMap.get(cjdid3);
                }else {
                    mildleFilteLong=1;
                }
                mildleFilterMap.put(cjdid3,mildleFilteLong);
            }

            filterMap.put(code3,mildleFilterMap);
        }

        System.out.println(filterMap.size());//421
        //过滤掉前二重复cjdid大于2，或前1cjd大于3
        List<String>  codeRFidList=new ArrayList<>();
        for (Map.Entry<String,Map<String,Integer>> entry:filterMap.entrySet()){
            //降序MAp
            TreeMap<Integer, String> orderMap = new TreeMap<Integer, String>(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return o2 - o1;
                }
            });
            for (Map.Entry<String,Integer> entry1:entry.getValue().entrySet()){
                orderMap.put(entry1.getValue(),entry1.getKey());
            }
            if(orderMap.size()>2){
                orderMap.remove(orderMap.lastKey());
            }

        *//*    codeRFidList.add(entry.getKey());
            System.out.println(entry.getKey());*//*
        //过滤
            if (*//*orderMap.lastKey()<2 && orderMap.firstKey()<3 && *//*orderMap.lastKey()!=orderMap.firstKey()){
                //这种车牌（RFID）是满足的
                codeRFidList.add(entry.getKey());
                System.out.println(entry.getKey());
            }
        }

        //过滤数据啦
        Map<String,String> finallyMap=new HashMap<>();
        for(Map.Entry<String, String> entry:outPutMap.entrySet()){
            if(codeRFidList.contains(entry.getKey().substring(0,entry.getKey().indexOf("-")))){
                finallyMap.put(entry.getKey(),entry.getValue());
            }
        }


        System.out.println(finallyMap.size());*/

        //写文档
        //RFID车牌，车牌颜色，抓拍车牌，抓拍车牌颜色，次数
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("套牌车");
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        cell.setCellValue("RFID车牌");
        cell = row.createCell(1);
        cell.setCellValue("车牌颜色编号");
        cell = row.createCell(2);
        cell.setCellValue("抓拍车牌");
        cell = row.createCell(3);
        cell.setCellValue("抓拍车牌颜色");
        cell = row.createCell(4);
        cell.setCellValue("次数");
        int count = 0;
        for (Map.Entry<Tuple<String,String>,Tuple<Tuple<String,String>,Integer>> entry : outPutMap.entrySet()) {
            HSSFRow row1 = sheet.createRow(count + 1);
            row1.createCell(0).setCellValue(entry.getKey().v1());
            row1.createCell(1).setCellValue(entry.getKey().v2());
            row1.createCell(2).setCellValue(entry.getValue().v1().v1());
            row1.createCell(3).setCellValue(entry.getValue().v1().v2());
            row1.createCell(4).setCellValue(entry.getValue().v2());
            count++;
        }
        try {
            FileOutputStream fos = new FileOutputStream("C:\\Users\\Administrator\\Desktop\\out.xls");
            workbook.write(fos);
            System.out.println("写入成功");
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }





    }

    //求一个map最大值
    private  String getMapMax( Map<String, Integer> map){
        int value=0;
        String maxKey = null;
        List list=new ArrayList();

        Iterator ite=map.entrySet().iterator();
        while(ite.hasNext()){
            Map.Entry entry =(Map.Entry)ite.next();
            value = Integer.parseInt(entry.getValue().toString());
            list.add(entry.getValue());
            Collections.sort(list);

            if(value == Integer.parseInt(list.get(list.size()-1).toString())){
                maxKey = entry.getKey().toString();
            }
        }
        return maxKey;
    }


    //取出一个集合中重复次数最多的字符串
    private String getMaxString(List<String> list) {
        Map<String, Long> top1 = new HashMap<>();
        Long aLong;
        for (String str : list) {
            if (top1.containsValue(str)) {
                aLong = top1.get(str);
                aLong += 1;
            } else {
                aLong = 1L;
            }
            top1.put(str, aLong);
        }
        SortedMap<Long, String> top = new TreeMap<Long, String>();
        for (Map.Entry<String, Long> entry : top1.entrySet()) {
            top.put(entry.getValue(), entry.getKey());
        }
        return top.get(top.lastKey());
    }

    //查找list中的数字与目标数最接近的数
    private Long getTime(List<Long> list, Long nearNum) {
        Long diffNum = Math.abs(list.get(0) - nearNum);
        // 最终结果
        Long result = list.get(0);
        for (Long integer : list) {
            Long diffNumTemp = Math.abs(integer - nearNum);
            if (diffNumTemp < diffNum) {
                diffNum = diffNumTemp;
                result = integer;
            }
        }
        return result;
    }


}
