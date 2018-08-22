package com.cigc.limit.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AppCfgUtils {

	private static Properties props = new Properties();
	private final static Log logger = LogFactory.getLog(AppCfgUtils.class);
	static {
		try {
			props.load(Thread.currentThread().getContextClassLoader()
							.getResourceAsStream("application.properties"));
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public static String get(String key) {
		return props.getProperty(key);
	}
	public static Properties getPros(){
		return props;
	}

	public static int getInt(String key){
		return Integer.parseInt(props.getProperty(key));
	}

	public static double getDouble(String key){
		return Double.parseDouble(props.getProperty(key));
	}
}
