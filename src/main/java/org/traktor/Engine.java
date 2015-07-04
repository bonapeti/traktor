package org.traktor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.traktor.domain.Observation;
import org.traktor.domain.Worker;
import org.traktor.domain.sampling.Sampling;
import org.traktor.domain.sampling.Scheduler;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selector;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;
import reactor.fn.Predicate;
import reactor.fn.Supplier;
import reactor.fn.timer.Timer;

@SpringBootApplication
public class Engine implements CommandLineRunner, ApplicationContextAware {
	
	@Autowired
	private EventBus workers;
	
	@Autowired
	private Worker worker;
	
	ApplicationContext applicationContext;
	
	static {
		Environment.initializeIfEmpty().assignErrorJournal();
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Engine.class, args);
	}

	@Autowired
	private Scheduler scheduler;
	
	@Autowired
	private Meter monitoringRequests;
	
	@Bean
	public EventBus eventBus() {
		return EventBus.create();
	}
	
	@Bean EventBus workers() {
		return EventBus.create(Environment.workDispatcher());
	}
	
	@Bean
	public Timer timer() {
		return Environment.timer();
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
	@Primary
	public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
		return new Jackson2ObjectMapperBuilder().serializerByType(Sampling.class, new JsonSerializer<Sampling<?>>() {

			@Override
			public void serialize(Sampling<?> sampling, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonProcessingException {
				jgen.writeStartObject();
		        jgen.writeStringField("name", sampling.getName());
		        Object lastValue = sampling.getLastValue();
		        if (lastValue != null) {
		        	jgen.writeObjectField("last", lastValue);
		        }
		        jgen.writeEndObject();
			}
			
		}).serializerByType(Observation.class, 	new JsonSerializer<Observation>() {

			@Override
			public void serialize(Observation observation, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonProcessingException {
				jgen.writeStartObject();
		        jgen.writeStringField("time", observation.getTime().toString());
		        Object value = observation.getValue();
		        if (value != null) {
		        	jgen.writeObjectField("value", value);
		        }
		        jgen.writeEndObject();
				
			}
			
		}).indentOutput(true);
	}
	
	
	@Override
	public void run(String... arg0) throws Exception {
		
		//eventBus.on(anyResult(), new EventPrinter());
		//eventBus.on(Selectors.$("traktor.local.internal.monitoringrequests.rate.oneminute.results"), new EventPrinter());
		workers.on(anyRequest(), worker);
		workers.on(anyRequest(), new Consumer<Event<Supplier<Object>>>() {
			
			@Override
			public void accept(Event<Supplier<Object>> t) {
				monitoringRequests.mark();
			}

		});

		scheduleInternalMonitoring();
	}

	private void scheduleInternalMonitoring() {
		long period = 10;
		
		scheduler.schedule("traktor.local.internal.items.count", new Supplier<Integer>() {

			@Override
			public Integer get() {
				return scheduler.size();
			}
			
		}, period);
		scheduler.schedule("traktor.local.internal.monitoringrequests.rate.mean", new Supplier<Double>() {

			@Override
			public Double get() {
				return monitoringRequests.getMeanRate();
			}
			
		}, period);
		scheduler.schedule("traktor.local.internal.monitoringrequests.rate.oneminute", new Supplier<Double>() {

			@Override
			public Double get() {
				return monitoringRequests.getOneMinuteRate();
			}
			
		}, period);
		scheduler.schedule("traktor.local.internal.monitoringrequests.rate.fiveminute", new Supplier<Double>() {

			@Override
			public Double get() {
				return monitoringRequests.getFiveMinuteRate();
			}
			
		}, period);
		scheduler.schedule("traktor.local.internal.monitoringrequests.rate.fifteenminute", new Supplier<Double>() {

			@Override
			public Double get() {
				return monitoringRequests.getFifteenMinuteRate();
			}
			
		}, period);
		scheduler.schedule("traktor.local.jvm.threadcount", new Supplier<Integer>() {

			@Override
			public Integer get() {
				return ManagementFactory.getThreadMXBean().getThreadCount();
			}
			
		}, period);
		scheduler.schedule("traktor.local.jvm.peakthreadcount", new Supplier<Integer>() {

			@Override
			public Integer get() {
				return ManagementFactory.getThreadMXBean().getPeakThreadCount();
			}
			
		}, period);
		scheduler.schedule("traktor.local.jvm.daemonthreadcount", new Supplier<Integer>() {

			@Override
			public Integer get() {
				return ManagementFactory.getThreadMXBean().getDaemonThreadCount();
			}
			
		}, period);
		scheduler.schedule("traktor.local.jvm.memory.heap", new Supplier<MemoryUsage>() {

			@Override
			public MemoryUsage get() {
				return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
			}
			
		}, period);
		scheduler.schedule("traktor.local.jvm.memory.nonheap", new Supplier<MemoryUsage>() {

			@Override
			public MemoryUsage get() {
				return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
			}
			
		}, period);
	}
	
	
	
	public static Selector<Object> anyResult() {
		return Selectors.predicate(new EndsWith(".results"));
	}
	
	public static Selector<Object> anyRequest() {
		return Selectors.predicate(new EndsWith(".requests"));
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}


}

class EndsWith implements Predicate<Object> {
	
	private final String end;
	
	public EndsWith(String end) {
		this.end = end;
	}
	
	@Override
	public boolean test(Object t) {
		if (!(String.class.isInstance(t))) {
			return false;
		}
		
		return ((String)t).endsWith(end);
	}
}

class EventPrinter implements Consumer<Event<Object>> {

	@Override
	public void accept(Event<Object> e) {
		System.out.println(e.getKey() + "=" + e.getData());
	}
}
