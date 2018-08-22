//package com.cigc.limit.utils;
//
//import com.alibaba.fastjson.JSONObject;
//import com.cigc.analysis.utils.DateUtils;
//import com.cigc.analysis.utils.PinyinUtil;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.client.transport.TransportClient;
//import org.elasticsearch.index.query.BoolQueryBuilder;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.index.query.RangeQueryBuilder;
//import org.elasticsearch.index.query.TermsQueryBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created by dc-dev on 2018/6/26.
// */
//@Component
//public class CountyCount {
//    @Autowired
//    private TransportClient client;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//
//    public void countyquery() {
//        System.out.println(DateUtils.getDateStr());
//        List<Map<String, Object>> areaMaps = jdbcTemplate.queryForList("SELECT area from station_static_info where area<> '' group by area");
//        for (Map<String, Object> areaMap : areaMaps) {
//            String area = (String) areaMap.get("area");
//            HashSet<String> ipSet = new HashSet<>();
//            List<Map<String, Object>> ipMaps = jdbcTemplate.queryForList("select reader_ip from station_static_info where area LIKE '%" + area + "%'");
//            for (Map<String, Object> ipMap : ipMaps) {
//                String readerIp = (String) ipMap.get("reader_ip");
//                //System.out.println(readerIp);
//                Collections.addAll(ipSet, readerIp.split(","));
//            }
//            long reidcount = queryEs("readerIP", "collectTime", ipSet, "RFID");
//            jdbcTemplate.execute("UPDATE screenshow.fence_amounts set amounts="+reidcount+",datetime='"+DateUtils.getDateStr()+"'  where dataname='rfid' and area='"+area+"' ");
//
//            HashSet<String> codeSet = new HashSet<>();
//            for (String ip : ipSet) {
//                String sqlzp = "SELECT z.DEVICE_CODE FROM  hc_zs_static_cjd_zp z left join hc_zs_static_cjd_rfid r on z.CJDID=r.CJDID WHERE  r.EQUIPMENT_RFID='" + ip.toString() + "'  ";
//                List<Map<String, Object>> zpMaps = jdbcTemplate.queryForList(sqlzp);
//                for (Map<String, Object> ipMap : zpMaps) {
//                    String zpcode = (String) ipMap.get("DEVICE_CODE");
//                    Collections.addAll(codeSet, zpcode);
//                }
//            }
//            long zpcount = queryEs("tollgateCode", "passTime", codeSet, "SNAPSHOT");
//            jdbcTemplate.execute("UPDATE screenshow.fence_amounts set amounts="+zpcount+",datetime='"+DateUtils.getDateStr()+"'  where dataname='snapshot' and area='"+area+"' ");
//
//
//
//
//        }
//    }
//
//
//        public long queryEs(String ip,String time,HashSet<String> ipSet,String type){
//           long start = DateUtils.getMillis(0,true);
//            long end = System.currentTimeMillis();
//            //从es查询数据存入mysql数据库
//            BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();
//            TermsQueryBuilder termsQuery = QueryBuilders.termsQuery(ip, ipSet);
//            RangeQueryBuilder startQuery = QueryBuilders.rangeQuery(time).gte(start);
//            RangeQueryBuilder endQuery = QueryBuilders.rangeQuery(time).lt(end);
//            rootQuery.must(termsQuery);
//            rootQuery.must(startQuery);
//            rootQuery.must(endQuery);
//            SearchResponse response = client.prepareSearch(DateUtils.getIndexName())
//                    .setTypes(type)
//                    .setQuery(rootQuery)
//                    .execute()
//                    .actionGet();
//
//            return response.getHits().getTotalHits();
//        }
//}
