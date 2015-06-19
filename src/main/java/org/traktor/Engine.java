package org.traktor;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.traktor.domain.Items;
import org.traktor.domain.Worker;
import org.traktor.domain.local.internal.NumberOfItems;
import org.traktor.domain.local.jvm.HeapMemory;
import org.traktor.domain.local.jvm.NonHeapMemory;
import org.traktor.domain.local.jvm.ThreadCount;

import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selector;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;
import reactor.fn.Pausable;
import reactor.fn.Predicate;
import reactor.fn.Supplier;
import reactor.fn.timer.Timer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

@SpringBootApplication
@RestController
public class Engine implements CommandLineRunner, ApplicationContextAware {
	
	@Autowired
	private EventBus eventBus;
	
	@Autowired
	private EventBus workers;
	
	@Autowired
	private Worker worker;
	
	@Autowired
	private MetricRegistry metrics;
	
	ApplicationContext applicationContext;
	
	static {
		Environment.initializeIfEmpty().assignErrorJournal();
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Engine.class, args);
	}

	@Autowired
	private Items items;
	
	@Autowired
	private NumberOfItems numberOfItems;
	
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

	@RequestMapping(method=RequestMethod.GET)
    public Collection<String> monitoredItem() {
        return items.getNames();
    }
	
	
	@Override
	public void run(String... arg0) throws Exception {
		
		eventBus.on(anyResult(), new Consumer<Event<Object>>() {

			@Override
			public void accept(Event<Object> e) {
				System.out.println(Thread.currentThread().getName() + " " + e);
			}
		});
		workers.on(anyRequest(), worker);

		long period = 10;
		
		items.addItem("traktor.local.internal.numberofitems", numberOfItems, period);
		items.addItem("traktor.local.jvm.threadcount", new ThreadCount(), period);
		items.addItem("traktor.local.jvm.heapmemory", new HeapMemory(), period);
		items.addItem("traktor.local.jvm.nonheapmemory", new NonHeapMemory(), period);
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

