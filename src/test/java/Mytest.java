import com.cigc.limit.service.TaoPai;
import com.cigc.limit.utils.LocationUtils;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.*;

/**
 * Created by Administrator on 2018/6/28 0028.
 */
public class Mytest {

    @Test
    public void test1() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://10.10.0.101:3306/zeus?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        // 创建JDBC模板
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        // 这里也可以使用构造方法
        jdbcTemplate.setDataSource(dataSource);

        // sql语句
        String sql = "select count(*)  from HC_ZS_STATIC_CJD_ZP";
        Long num = (long) jdbcTemplate.queryForObject(sql, Long.class);

        System.out.println(num);
    }

    @Test
    public void test2(){
        List<String> list=new ArrayList<>();
        list.add("ssss");
        list.add("ssss");
        list.add("q");
        list.add("ssddss");
        list.add("ssss");
        list.add("q");
        list.add("ssss");
        TaoPai taoPai=new TaoPai();
//        System.out.println(taoPai.getMaxString(list));

    }

    @Test
    public  void  test3(){
        Map map=new HashMap();
        map.put("d", 765);
        map.put("g", 7);
        map.put("a", 761);
        map.put("c", 34);
        TaoPai taoPai=new TaoPai();
//        System.out.println(taoPai.getMapMax(map));

    }

    //treemap降序
    @Test
    public  void  test4(){
        Map<Integer, Object> map = new TreeMap<Integer, Object>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });

        map.put(1, 111);
        map.put(2, 222);
        map.put(3, 333);
        map.put(4, 444);
        map.put(5, 555);

        System.out.println(map);

    }

    @Test
    public  void  test5(){
        Date date=new Date();
        int hours=date.getHours();
        int miute=date.getMinutes();
        System.out.println(hours+"------------"+miute);
    }

    @Test
    public void  test6(){

        double s = 0d;
        s = LocationUtils.getDistance(29.538312601173,106.719661891502,29.56569550425,106.532930435108);
        System.out.println(s);

    }

}
