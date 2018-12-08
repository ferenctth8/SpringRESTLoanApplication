package com.feritoth.cla.springmvc.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;

@Configuration
@EnableWebMvc
public class HibernateAwareObjectMapper extends ObjectMapper {
	
	private static final long serialVersionUID = 1L;

	public HibernateAwareObjectMapper() {
		registerModule(new Hibernate4Module());
	}

}