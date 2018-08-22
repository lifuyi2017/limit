package com.cigc.limit.service;

import java.net.InetAddress;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

	private final static Logger logger = LoggerFactory.getLogger(ElasticSearchConfig.class);
	
	@Bean(name="client")
	public TransportClient createTransportClient() throws Exception {
				
        Settings settings = Settings.builder()
                .put("cluster.name","cq-search") 
                .put("client.transport.sniff",true)
                .build();
        
        TransportClient client = new PreBuiltTransportClient(settings)
        		.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.0.0.129"), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.0.0.130"), 9300))
		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.0.0.131"), 9300))
		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.0.0.132"), 9300))
		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.0.0.133"), 9300));
        
        logger.info("elasticsearch client initialized !!!");
        return client;
	}
	
}
