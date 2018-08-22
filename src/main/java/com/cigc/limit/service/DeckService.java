package com.cigc.limit.service;

import com.cigc.limit.domain.Area;
import com.cigc.limit.domain.AreaMapper;
import com.cigc.limit.domain.Location;
import com.cigc.limit.domain.LocationMapper;
import com.cigc.limit.utils.AppCfgUtils;
import com.cigc.limit.utils.DateUtils;
import com.cigc.limit.utils.FileUtils;
import com.cigc.limit.utils.LocationUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2018/7/2 0002.
 * 套牌车业务
 */
@Component
public class DeckService {
    @Autowired
    private TransportClient client;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Integer buquanTime = 1;

    private static double velocity = AppCfgUtils.getDouble("velocity");

    private static Map<String, Location> cjdidMap;

    private static String index = "cqct_20180508_*";

    private static String type = "AfterVehicle";

    private static Map<String, Area> areaAllMap;

    private static Map<String, String> codeInputMap;

    private  static  ExecutorService es = Executors.newCachedThreadPool();

    private static  CountDownLatch latch;

    public void searchData() {
        
        
        

        //查询经纬度
        cjdidMap = new HashMap<>();
        //地点信息
        areaAllMap = new HashMap<>();
        String sql1 = "select DEVICE_CODE from hc_zs_static_cjd_zp ";
        List<String> cjdidList = jdbcTemplate.queryForList(sql1, String.class);
        for (String str : cjdidList) {
            Object[] object = {str};
            String sql2 = "SELECT count(1) FROM station_static_info WHERE dir_code=(SELECT direction_code from bm_device " +
                    "where ip=(SELECT EQUIPMENT_RFID from hc_zs_static_cjd_rfid where CJDID=(select CJDID from hc_zs_static_cjd_zp where DEVICE_CODE=? limit 1) limit 1) limit 1)";
            Long num = (long) jdbcTemplate.queryForObject(sql2, object, Long.class);
            if (num != 0) {
                String sql3 = "SELECT amap_latitude,amap_longitude FROM station_static_info WHERE " +
                        "dir_code=(SELECT direction_code from bm_device where ip=(SELECT EQUIPMENT_RFID from hc_zs_static_cjd_rfid " +
                        "where CJDID=(select CJDID from hc_zs_static_cjd_zp where DEVICE_CODE=? limit 1) limit 1) limit 1)";
                Location location = jdbcTemplate.queryForObject(sql3, object, new LocationMapper());

                if (location.getLat() == 0.0 || location.getLng() == 0.0) {
                    System.out.println(location.getLat() + "----------------" + location.getLng());
                } else if(location.getLat() != 0.0 && location.getLng() != 0.0){
                    cjdidMap.put(str, location);
                }

            }


            String sql4 = "SELECT count(1) FROM hc_zs_static_collect_point WHERE CJDID=?";
            num = jdbcTemplate.queryForObject(sql4, object, Long.class);
            if (num != 0) {
                String sql5 = "SELECT FX,CJDNAME FROM hc_zs_static_collect_point WHERE CJDID=? limit 1";
                Area area = jdbcTemplate.queryForObject(sql5, object, new AreaMapper());
                areaAllMap.put(str, area);
            }
        }

        //记录异常CJDID(缺失了经纬度)
/*        for(String str:cjdidList){
            if(!cjdidMap.containsKey(str)){
                FileUtils.write("D:\\异常CJDID点.txt", str);
            }
        }*/


        //查询补齐点
        String[] buquan = {"0001", "0011", "0012", "1011"};

        //建立查询条件
        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
        //融合成功数据
        ExistsQueryBuilder exit1 = QueryBuilders.existsQuery("snapsotId");
        ExistsQueryBuilder exit = QueryBuilders.existsQuery("platePic");

        ExistsQueryBuilder exit2 = QueryBuilders.existsQuery("readerIP");
        //时间
        Date date = new Date();
        int hours = date.getHours();
        int miute = date.getMinutes();
//        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getTimeStmap(-(hours*60+miute+720)));
//        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getTimeStmap(-(hours*60+miute+700)));
        RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getMillis(-AppCfgUtils.getInt("starttime"), true));
        RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getMillis(-AppCfgUtils.getInt("endtime"), true));
        //电子牌补全代码
        TermsQueryBuilder rectifyCode = QueryBuilders.termsQuery("rectifyCode", buquan);
        //根据车牌分组
        AggregationBuilder groupTerms = AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                .subAggregation(AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE));


        //补全数据
        rootQuery.must(exit1).must(exit2).must(startQuery).must(endQuery).must(rectifyCode);

        SearchResponse response = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(rootQuery)
                .addAggregation(groupTerms)
                .setExplain(true).execute().actionGet();


        String code = "";
        Integer times = 0;
        String color = "";
        //查询结果:补全次数过多的车辆
        //key：车牌号+颜色，补全次数
        Map<Tuple<String, String>, Integer> queryMap = new HashMap<>();

        Terms groupCode = response.getAggregations().get("groupCode");
        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
            code = codeBucket.getKeyAsString();
            Terms groupColor = codeBucket.getAggregations().get("groupColor");
            for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                color = colorBucket.getKeyAsString();
                if (colorBucket.getDocCount() > buquanTime) {
                    queryMap.put(new Tuple<>(code, color), Math.toIntExact(codeBucket.getDocCount()));
                }
            }
        }
        System.out.println(queryMap.size());


        //仅有抓拍的
        Map<String, String> snapshotMap = new HashMap<>();


        BoolQueryBuilder snapshotQuery = QueryBuilders.boolQuery();

        snapshotQuery.must(startQuery).must(endQuery).must(QueryBuilders.existsQuery("snapsotId"))
                .mustNot(QueryBuilders.existsQuery("readerIP"));

        response = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(snapshotQuery)
                .addAggregation(groupTerms)
                .setExplain(true).execute().actionGet();


        groupCode = response.getAggregations().get("groupCode");
        for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
            code = codeBucket.getKeyAsString();
            Terms groupColor = codeBucket.getAggregations().get("groupColor");
            for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                color = colorBucket.getKeyAsString();
                if (code.substring(0, 1).equals("渝")) {
//                    System.out.println(code);
                    snapshotMap.put(code, color);


/*                    if (snapshotMap.size() == 10000) {

                        beginCal(queryMap, snapshotMap,es);

                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        snapshotMap=new HashMap<>();
                        System.out.println("处理完10000条了");
                    }*/


                }
            }
        }

        beginCal(queryMap, snapshotMap,es);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    //每到1万计算
    private void beginCal(Map<Tuple<String, String>, Integer> queryMap, Map<String, String> snapshotMap,  ExecutorService es) {
        //车牌合并
        for (Map.Entry<Tuple<String, String>, Integer> entry : queryMap.entrySet()) {
            snapshotMap.put(entry.getKey().v1(), entry.getKey().v2());
        }

        System.out.println(snapshotMap.size());


        //线程数量，一个线程处理500条
        int threadNum = (snapshotMap.size()) / 10000 + 1;
        System.out.println(threadNum);
        int count = 0;

        latch = new CountDownLatch(5);

        codeInputMap = new HashMap<>();


        //构造线程集合
        for (Map.Entry<String, String> entry : snapshotMap.entrySet()) {
            codeInputMap.put(entry.getKey(), entry.getValue());
            count++;
            if (count == 10000) {
                System.out.println(codeInputMap.size());

                Map<String, String> InputMap = codeInputMap;
                Map<String, Location> cjidMapIn = cjdidMap;
                Map<String, Area> areaAllMapin = areaAllMap;
                TransportClient clientIn = client;
                double Vin = velocity;
                String indexIn = index;
                String typeIn = type;

                Param param = new Param(InputMap, cjidMapIn, areaAllMapin, clientIn, Vin, indexIn, typeIn);
                Task task = new Task(param, latch);
                es.submit(task);

                count = 0;
                //这里不要进行MAP清空操作，否则会影响已提交的任务
                codeInputMap = new HashMap<>();
            }
        }
        Map<String, String> InputMap = codeInputMap;
        Map<String, Location> cjidMapIn = cjdidMap;
        Map<String, Area> areaAllMapin = areaAllMap;
        TransportClient clientIn = client;
        double Vin = velocity;
        String indexIn = index;
        String typeIn = type;
        System.out.println(InputMap.size());

        Param param = new Param(InputMap, cjidMapIn, areaAllMapin, clientIn, Vin, indexIn, typeIn);
        Task task = new Task(param, latch);
        es.submit(task);

    }


    //计算最大速度
    private Double getVelocity(TreeMap<Long, Location> locationMap) {
        Double maxV = 0.0;
        Long lastTime = 0L;
        Location lastLocation = new Location();
        for (Map.Entry<Long, Location> entry : locationMap.entrySet()) {
            if (lastTime != 0) {
                Double v = LocationUtils.getDistance(entry.getValue().getLat(), entry.getValue().getLng(), lastLocation.getLat(), lastLocation.getLng()) / (entry.getKey() - lastTime);
//                System.out.println("速度：" + v);
                if (v > maxV) {
                    maxV = v;
                }
            }
            lastTime = entry.getKey();
            lastLocation = entry.getValue();
        }
//        System.out.println("最大速度" + maxV);
        return maxV;

    }
}


class Param {
    private Map<String, String> carCodeMap;
    private Map<String, Location> cjdidMap;
    private Map<String, Area> areaAllMap;
    private TransportClient client;
    private double velocity;
    private String index;
    private String type;

    public Param() {
    }

    public Param(Map<String, String> carCodeMap, Map<String, Location> cjdidMap,
                 Map<String, Area> areaAllMap, TransportClient client, double velocity, String index, String type) {
        this.carCodeMap = carCodeMap;
        this.cjdidMap = cjdidMap;
        this.areaAllMap = areaAllMap;
        this.client = client;
        this.velocity = velocity;
        this.index = index;
        this.type = type;
    }


    public Map<String, String> getCarCodeMap() {
        return carCodeMap;
    }

    public void setCarCodeMap(Map<String, String> carCodeMap) {
        this.carCodeMap = carCodeMap;
    }

    public Map<String, Location> getCjdidMap() {
        return cjdidMap;
    }

    public void setCjdidMap(Map<String, Location> cjdidMap) {
        this.cjdidMap = cjdidMap;
    }

    public Map<String, Area> getAreaAllMap() {
        return areaAllMap;
    }

    public void setAreaAllMap(Map<String, Area> areaAllMap) {
        this.areaAllMap = areaAllMap;
    }

    public TransportClient getClient() {
        return client;
    }

    public void setClient(TransportClient client) {
        this.client = client;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

class Task implements Runnable {
    private Param param;
    private CountDownLatch latch;

    public Task(Param param, CountDownLatch latch) {
        this.param = param;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getMillis(-AppCfgUtils.getInt("starttime"), true));
            RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getMillis(-AppCfgUtils.getInt("endtime"), true));

            Date date = new Date();
            int hours = date.getHours();
            int miute = date.getMinutes();
//            RangeQueryBuilder startQuery = QueryBuilders.rangeQuery("passTime").from(DateUtils.getTimeStmap(-(hours*60+miute+720)));
//            RangeQueryBuilder endQuery = QueryBuilders.rangeQuery("passTime").to(DateUtils.getTimeStmap(-(hours*60+miute+700)));

            ExistsQueryBuilder exit1 = QueryBuilders.existsQuery("snapsotId");
            ExistsQueryBuilder exit = QueryBuilders.existsQuery("platePic");
            //查询轨迹，从有CJDID的数据中查找数据
            SearchHits hits;
            TreeMap<Long, String> trajectoryValue = new TreeMap<>();

            BoolQueryBuilder trajectoryQuery = QueryBuilders.boolQuery();
            TermsQueryBuilder codeQuery = QueryBuilders.termsQuery("plateCode", param.getCarCodeMap().keySet());
            //根据车牌分组
            AggregationBuilder trajectoryTerms = AggregationBuilders.terms("groupCode").field("plateCode").size(Integer.MAX_VALUE)
                    .subAggregation(AggregationBuilders.terms("groupColor").field("plateColor").size(Integer.MAX_VALUE)
                            .subAggregation(AggregationBuilders.terms("groupTime").field("passTime").size(Integer.MAX_VALUE)
                                    .subAggregation(AggregationBuilders.terms("groupCjdid").field("tollgateCode").size(Integer.MAX_VALUE))));

            trajectoryQuery.filter(codeQuery);

            trajectoryQuery.must(startQuery)
                    .must(endQuery)
                    .must(exit1).must(exit);

            SearchResponse response = param.getClient().prepareSearch(param.getIndex())
                    .setTypes(param.getType())
                    .setQuery(trajectoryQuery)
                    .addAggregation(trajectoryTerms)
                    .setExplain(true).execute().actionGet();

            //查询轨迹
            Long time;
            String ip = "";
            String cjdid = "";
            String code = "";
            String color = "";
            //key：车牌号+颜色，value：时间，cjdid
            Map<Tuple<String, String>, TreeMap<Long, String>> trajectoryMap = new HashMap<>();
            Terms groupCode = response.getAggregations().get("groupCode");
            for (Terms.Bucket codeBucket : groupCode.getBuckets()) {
                code = codeBucket.getKeyAsString();
                Terms groupColor = codeBucket.getAggregations().get("groupColor");
                for (Terms.Bucket colorBucket : groupColor.getBuckets()) {
                    color = colorBucket.getKeyAsString();
                    //车牌与颜色是否匹配
                    if (color.equals(param.getCarCodeMap().get(code))) {
//                    System.out.println("==================" + code + "==============" + color + "=======================");
                        //放数据
                        trajectoryValue = new TreeMap<>();
                        Terms groupTime = colorBucket.getAggregations().get("groupTime");
                        for (Terms.Bucket timeBucket : groupTime.getBuckets()) {
                            time = (Long) timeBucket.getKey();
                            Terms groupCjdid = timeBucket.getAggregations().get("groupCjdid");
                            for (Terms.Bucket cjdidBucket : groupCjdid.getBuckets()) {
                                cjdid = cjdidBucket.getKeyAsString();
                                trajectoryValue.put(time, cjdid);
//                            System.out.println(time + "-------------------------" + cjdid);
                            }
                        }
//                    System.out.println("========================================================================");

                    }
                }
                trajectoryMap.put(new Tuple<>(code, color), trajectoryValue);
            }

            System.out.println("轨迹点集合大小：" + trajectoryMap.size());


//        System.out.println("轨迹数据查询完毕====================开始计算速度");
            //key：车牌号+颜色，value：时间，readerIp
//        Map<Tuple<String, String>, TreeMap<Long, String>> trajectoryMap
            //速度判定

            //一：计算每个点经纬度缺失的次数，如果大于3次，记录，这种打印点坐标详情
            //key：车牌号+颜色，value：时间，cjdid
            Map<Tuple<String, String>, TreeMap<Long, String>> LocatinMap = new HashMap<>();
            Map<Tuple<String, String>, TreeMap<Long, String>> VelocityMap = new HashMap<>();

            for (Map.Entry<Tuple<String, String>, TreeMap<Long, String>> entry : trajectoryMap.entrySet()) {
                String sql = "";
                Integer count = 0;
                for (Map.Entry<Long, String> entry1 : entry.getValue().entrySet()) {
                    if (!param.getCjdidMap().containsKey(entry1.getValue())) {
                        count++;
                    }
                    if (count > 3 || count == 3) {
                        LocatinMap.put(entry.getKey(), entry.getValue());
                        break;
                    }
                }
                //经纬度缺失小于3次
                if (count < 3) {
                    VelocityMap.put(entry.getKey(), entry.getValue());
                }
            }

//        System.out.println(LocatinMap.size()+"=========="+VelocityMap.size()+"============"+trajectoryMap.size());

            //详情点:key:车牌+颜色，value：时间,经纬度，地点方向，
           /* Map<Tuple<String, String>, TreeMap<Long, Tuple<Tuple<Double, Double>, Tuple<String, String>>>> LocatinOutMap = new HashMap<>();
            for (Map.Entry<Tuple<String, String>, TreeMap<Long, String>> entry : LocatinMap.entrySet()) {
                String sql = "";
                TreeMap<Long, Tuple<Tuple<Double, Double>, Tuple<String, String>>> areaMap = new TreeMap<>();
                for (Map.Entry<Long, String> entry1 : entry.getValue().entrySet()) {
                    //经纬度处理
                    Tuple<Double, Double> lantlng = null;
                    if (param.getCjdidMap().containsKey(entry1.getValue())) {
                        Location location = param.getCjdidMap().get(entry1.getValue());
                        lantlng = new Tuple<>(location.getLat(), location.getLng());
                    } else {
                        lantlng = new Tuple<>(0.0, 0.0);
                    }
                    //地点信息处理
                    Tuple<String, String> locationA = null;
                    if (param.getAreaAllMap().containsKey(entry1.getValue())) {
                        Area area = param.getAreaAllMap().get(entry1.getValue());
                        locationA = new Tuple<>(area.getFX(), area.getWeizhi());
                    } else {
                        locationA = new Tuple<>("", "");
                    }
                    areaMap.put(entry1.getKey(), new Tuple<>(lantlng, locationA));
                }

                LocatinOutMap.put(entry.getKey(), areaMap);
            }*/


            //详情点:key:车牌+颜色，value：时间,时间差,经纬度，地点方向，
       /*     Map<Tuple<String, String>, HashMap<Tuple<Long, Long>, Tuple<Tuple<Double, Double>, Tuple<String, String>>>> fianllyOutArea = new HashMap<>();
            for (Map.Entry<Tuple<String, String>, TreeMap<Long, Tuple<Tuple<Double, Double>, Tuple<String, String>>>> entry : LocatinOutMap.entrySet()) {
                Long lastTime = 0L;
                HashMap<Tuple<Long, Long>, Tuple<Tuple<Double, Double>, Tuple<String, String>>> areaMap = new HashMap<>();
                for (Map.Entry<Long, Tuple<Tuple<Double, Double>, Tuple<String, String>>> entry1 : entry.getValue().entrySet()) {
                    if (lastTime == 0L) {
                        areaMap.put(new Tuple<>(entry1.getKey(), lastTime), entry1.getValue());
                    } else {
                        areaMap.put(new Tuple<>(entry1.getKey(), entry1.getKey() - lastTime), entry1.getValue());
                    }
                    lastTime = entry1.getKey();

                }
                fianllyOutArea.put(entry.getKey(), areaMap);
            }*/

            //详情点:key:车牌+颜色，value：时间,时间差,经纬度，地点方向，
/*
        for(Map.Entry<Tuple<String, String>, HashMap<Tuple<Long, Long>, Tuple<Tuple<Double, Double>, Tuple<String, String>>>> entry:fianllyOutArea.entrySet()){
            FileUtils.write("D:\\"+AppCfgUtils.get("file"), entry.getKey().v1() + "\t" + entry.getKey().v2()+"============================");
            for(Map.Entry<Tuple<Long, Long>, Tuple<Tuple<Double, Double>, Tuple<String, String>>> entry1:entry.getValue().entrySet()){
                FileUtils.write("D:\\"+AppCfgUtils.get("file"), entry1.getKey().v1()+"                  "+entry1.getKey().v2()/10000+"         "+entry1.getValue().v1().v1()+
                        "                  "
                        +entry1.getValue().v1().v2()+"             "+entry1.getValue().v2().v1()+"                    "+entry1.getValue().v2().v2());
            }
            FileUtils.write("D:\\"+AppCfgUtils.get("file"), "================================================");
        }
*/

            System.out.println("速度计算点集合" + VelocityMap.size());


            //key：车牌号+颜色，value：map:时间+经纬度，最大速度+次数+最大速度出现时间点
            Map<Tuple<String, String>, Tuple<TreeMap<Long,Location>, Tuple<Double,Tuple<Integer,String>>>> output = new HashMap<>();

            //计算速度
            for (Map.Entry<Tuple<String, String>, TreeMap<Long, String>> entry : VelocityMap.entrySet()) {
                String sql = "";
                TreeMap<Long, Location> locationMap = new TreeMap<>();
                //某辆车轨迹
                for (Map.Entry<Long, String> entry1 : entry.getValue().entrySet()) {
                    if (param.getCjdidMap().containsKey(entry1.getValue())) {
                        Location location = param.getCjdidMap().get(entry1.getValue());
                        locationMap.put(entry1.getKey(), location);
                    }
                }

                Double maxV = 0.0;
                Long lastTime = 0L;
                Location lastLocation = new Location();
                Integer Vtime = 0;
                String dateTime ="";
                //速度计算方法
                for (Map.Entry<Long, Location> entry0 : locationMap.entrySet()) {
                    if (lastTime != 0) {
                        Double v = LocationUtils.getDistance(entry0.getValue().getLat(), entry0.getValue().getLng(), lastLocation.getLat(), lastLocation.getLng()) / (entry0.getKey() - lastTime);
//                    System.out.println("速度：" + v);
                        if (v > param.getVelocity()) {
                            Vtime += 1;
                        }
                        if (v > maxV) {
                            maxV = v;
                            dateTime=getDate(entry0.getKey());
                        }
                    }
                    lastTime = entry0.getKey();
                    lastLocation = entry0.getValue();
                }
//            System.out.println("最大速度" + maxV);
                //超速大于3次
                if (Vtime > 1) {
                    //这些点的详情
                    output.put(entry.getKey(), new Tuple<>(locationMap, new Tuple<>(maxV,new Tuple<>(Vtime,dateTime))));
                }
            }
            System.out.println("结果" + output.size());
            for (Map.Entry<Tuple<String, String>, Tuple<TreeMap<Long,Location>,Tuple<Double,Tuple<Integer,String>>>> entry : output.entrySet()) {
                FileUtils.write("D:\\"+AppCfgUtils.get("file"), entry.getKey().v1() + "\t" + entry.getKey().v2() + "\t"
                        + entry.getValue().v2().v1()*3600+"\t"+entry.getValue().v2().v2().v1()+"\t"+entry.getValue().v2().v2().v2());
//                FileUtils.write("D:\\"+AppCfgUtils.get("file"), entry.getKey().v1() + "," + entry.getKey().v2());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("本线程写入成功-------------------");
            latch.countDown();
        }

    }

    public String getDate(Long time){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//这个是你要转成后的时间的格式
        String sd = sdf.format(new Date(time));   // 时间戳转换成时间
        return sd;
    }
}