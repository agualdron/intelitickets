package com.empresa.tickets.config;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.data.web.config.SpringDataWebConfiguration;

@Configuration(proxyBeanMethods = false)
public class PaginationWebConfig extends SpringDataWebConfiguration {

	public PaginationWebConfig(ApplicationContext applicationContext,
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {
		super(applicationContext, conversionService);
	}

	@Override
	@Bean
	public PageableHandlerMethodArgumentResolver pageableResolver() {
		SortHandlerMethodArgumentResolver sortResolver = sortResolver();
		TicketPageableHandlerMethodArgumentResolver resolver =
			new TicketPageableHandlerMethodArgumentResolver(sortResolver);
		customizePageableResolver(resolver);
		return resolver;
	}

	@Bean
	PageableHandlerMethodArgumentResolverCustomizer pageableHandlerMethodArgumentResolverCustomizer() {
		return resolver -> resolver.setMaxPageSize(1000);
	}
}
