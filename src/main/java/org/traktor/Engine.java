package org.traktor;

import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.traktor.domain.HeapMemory;
import org.traktor.domain.Worker;

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
	private EventBus eventBus;
	
	@Autowired
	private EventBus workers;
	
	@Autowired
	private Timer timer;
	
	@Autowired
	private Worker worker;
	
	ApplicationContext applicationContext;
	
	static {
		Environment.initializeIfEmpty().assignErrorJournal();
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Engine.class, args);
	}

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

	
	@Override
	public void run(String... arg0) throws Exception {

		eventBus.on(Selectors.predicate(new Predicate<Object>() {
			
			@Override
			public boolean test(Object t) {
				if (!(String.class.isInstance(t))) {
					return false;
				}
				
				return ((String)t).endsWith(".results");
			}
		}), new Consumer<Event<Object>>() {

			@Override
			public void accept(Event<Object> e) {
				System.out.println(Thread.currentThread().getName() + " " + e);
			}
		});
		workers.on(Selectors.$(worker), worker);
		timer.schedule(new MonitoringRequestFactory<MemoryUsage>(new HeapMemory(), "traktor.local.jvm.heapmemory.used", worker, workers) ,10, TimeUnit.SECONDS);
	}

	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}


}

class NameEndsWithPredicate implements Predicate<Object> {
	
	private final String end;
	
	public NameEndsWithPredicate(String end) {
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

class MonitoringRequestFactory<T> implements Consumer<Long> {

	private final Supplier<T> item;
	private final String name;
	private final Worker worker;
	private final EventBus workers;
	
	public MonitoringRequestFactory(Supplier<T> item, String name, Worker worker, EventBus workers) {
		super();
		this.item = item;
		this.name = name;
		this.worker = worker;
		this.workers = workers;
	}

	@Override
	public void accept(Long t) {
		workers.notify(worker, Event.wrap(item, name + ".results"));
	}
	
}