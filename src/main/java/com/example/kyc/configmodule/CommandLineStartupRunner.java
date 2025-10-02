package com.example.kyc.configmodule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

//@Component
public class CommandLineStartupRunner implements CommandLineRunner {
	
	private static final Logger log = LoggerFactory.getLogger(CommandLineStartupRunner.class);
	
//    @Resource
//    private FilesStorageService storageService;
	
	@Value("${spring.profiles.active}")
	public String springProfiles;
	
	@Value("${app.baseUrl}")
	private String baseUrl;
	
	@Value("${app.swaggerUrl}")
	private String swaggerUrl;
	
	@Value("${greetings}")
	private String greetings;
	
    @Override
    public void run(String...args) throws Exception {
    	//storageService.init();
		log.debug("--------------" + greetings + "-------------------- Profile: ["+springProfiles+"]");
		log.debug("-------------- devedev BaseUrl -------------------- "+ baseUrl);
		log.debug("-------------- Swagger URL -------------------- "+ baseUrl+swaggerUrl);
    }
    
}
