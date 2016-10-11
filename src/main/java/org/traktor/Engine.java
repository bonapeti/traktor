package org.traktor;

import java.lang.management.ManagementFactory;

import org.apache.catalina.mbeans.MBeanFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.traktor.domain.Request;
import org.traktor.domain.sampling.Scheduler;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import reactor.core.publisher.TopicProcessor;


@SpringBootApplication
public class Engine extends WebMvcConfigurerAdapter implements CommandLineRunner, ApplicationContextAware {
	
	ApplicationContext applicationContext;
	
	public static void main(String[] args) {
		SpringApplication.run(Engine.class, args);
	}
	
	@Autowired
	private TopicProcessor<Request<?>> requestTopic;
	
	@Autowired
	private Scheduler scheduler;
	
	@Autowired
	private Meter monitoringRequests;
	
	@Bean
	public TopicProcessor<Request<?>> requestTopic() {
		return TopicProcessor.share("requestTopic", 256);
	}
	
	@Bean
	public MetricRegistry metrics() {
		return new MetricRegistry();
	}
	
	@Bean
	public Meter monitoringRequests() {
		return metrics().meter("monitoringRequests");
	}
	
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setUseSuffixPatternMatch(false);
	}
	
	
	@Override
	public void run(String... arg0) throws Exception {
		
		requestTopic.subscribe(i -> monitoringRequests.mark()); 
		
		long period = 10;
		
		//scheduler.schedule("traktor.local.internal.request.rate.15min", () -> monitoringRequests.getFifteenMinuteRate() , period);
		//scheduler.schedule("traktor.local.internal.request.rate.5min", () -> monitoringRequests.getFiveMinuteRate() , period);
		//scheduler.schedule("traktor.local.internal.request.rate.1min", () -> monitoringRequests.getOneMinuteRate(), period);
		//scheduler.schedule("traktor.local.internal.request.rate.mean", () -> monitoringRequests.getMeanRate(), period);
		scheduler.schedule("traktor.local.internal.jvm.os.systemload", () -> ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() , period);
		scheduler.schedule("traktor.test", () -> 1000, period);
		scheduler.schedule("traktor.local.internal.items.count", () -> scheduler.size() , period);
		
	}

	
		@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}


}

