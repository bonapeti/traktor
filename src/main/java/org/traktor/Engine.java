package org.traktor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

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
import org.traktor.domain.Observation;
import org.traktor.domain.influxdb.InfluxDBOutput;
import org.traktor.domain.net.OpenSocket;
import org.traktor.domain.sampling.Scheduler;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;

import reactor.core.publisher.TopicProcessor;
import reactor.util.concurrent.WaitStrategy;


@SpringBootApplication
public class Engine extends WebMvcConfigurerAdapter implements CommandLineRunner, ApplicationContextAware {
	
	ApplicationContext applicationContext;
	
	public static void main(String[] args) {
		SpringApplication.run(Engine.class, args);
	}
	
	@Autowired
	private TopicProcessor<Observation> resultTopic;
	
	@Autowired
	private MetricRegistry metrics;
	
	@Autowired
	private Scheduler scheduler;
	
	@Autowired
	private InfluxDBOutput influxDBOutput;
	
	
	
	@Autowired
	private Meter monitoringRequests;
	
	@Autowired
	private Meter monitoringErrors;
	
	
	@Bean
	public TopicProcessor<Observation> resultTopic() {
		return TopicProcessor.<Observation>builder().
				name("resultTopic").
				share(true).
				bufferSize(1024).			
				waitStrategy(WaitStrategy.blocking()).
				build();
	}
	
	@Bean
	public MetricRegistry metrics() {
		return new MetricRegistry();
	}
	
	@Bean
	public Meter monitoringRequests() {
		return metrics().meter("monitoringRequests");
	}
	
	@Bean
	public Meter monitoringErrors() {
		return metrics().meter("monitoringErrors");
	}
	
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setUseSuffixPatternMatch(false);
	}
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

	
	@Override
	public void run(String... arg0) throws Exception {
		JmxReporter.forRegistry(metrics).build().start();
		resultTopic.subscribe(influxDBOutput);
		
		//requestTopic.subscribe(i -> monitoringRequests.mark()); 
		
		long period = 10;
		
		scheduler.schedule("traktor.local.internal.request.rate.15min", () -> monitoringRequests.getFifteenMinuteRate() , period);
		scheduler.schedule("traktor.local.internal.request.rate.5min", () -> monitoringRequests.getFiveMinuteRate() , period);
		scheduler.schedule("traktor.local.internal.request.rate.1min", () -> monitoringRequests.getOneMinuteRate(), period);
		scheduler.schedule("traktor.local.internal.request.rate.mean", () -> monitoringRequests.getMeanRate(), period);
		
		scheduler.schedule("traktor.local.internal.errors.rate.15min", () -> monitoringErrors.getFifteenMinuteRate() , period);
		scheduler.schedule("traktor.local.internal.errors.rate.5min", () -> monitoringErrors.getFiveMinuteRate() , period);
		scheduler.schedule("traktor.local.internal.errors.rate.1min", () -> monitoringErrors.getOneMinuteRate(), period);
		scheduler.schedule("traktor.local.internal.errors.rate.mean", () -> monitoringErrors.getMeanRate(), period);
		
		
		
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			if (gc.isValid()) {
				final String name = WHITESPACE.matcher(gc.getName()).replaceAll("-");
				scheduler.schedule("traktor.local.internal.jvm.gc." + name + ".time", () -> gc.getCollectionTime(), period);
				scheduler.schedule("traktor.local.internal.jvm.gc." + name + ".count", () -> gc.getCollectionCount(), period);
			}
			
		}
		scheduler.schedule("traktor.local.internal.jvm.threads.count", () -> ManagementFactory.getThreadMXBean().getThreadCount() , period);
		scheduler.schedule("traktor.local.internal.items.count", () -> scheduler.size() , period);

		scheduler.schedule("msci.ftp.msci.com.open", new OpenSocket(new InetSocketAddress("ftp.msci.com", 21), 1000) , period);

		
	}

	
		@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}


}

