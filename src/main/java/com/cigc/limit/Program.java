package com.cigc.limit;

import com.cigc.limit.service.DeckService;
import com.cigc.limit.service.DeckService2;
import com.cigc.limit.service.DeckService3;
import com.cigc.limit.service.TaoPai;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class Program {
	private final static Log logger = LogFactory.getLog(Program.class);

	public static void main(String[] args) {

		try {
			@SuppressWarnings("unused")
			ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
			logger.info("----------Spring初始化完毕，开始分析统计任务----------");
			//AnalysisService analysisService = (AnalysisService) applicationContext.getBean("analysisService");
			//analysisService.noRfid();
//			DemoService demoService=new DemoService();
			DeckService deckService=(DeckService) applicationContext.getBean("deckService");
			deckService.searchData();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

}
