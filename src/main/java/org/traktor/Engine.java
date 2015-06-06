package org.traktor;

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

import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;
import reactor.fn.timer.Timer;

@SpringBootApplication
public class Engine implements CommandLineRunner, ApplicationContextAware {
	
	@Autowired
	private EventBus eventBus;
	
	@Autowired
	private EventBus workers;
	
	@Autowired
	private Timer timer;
	
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

		workers.on(Selectors.$("traktor.local.jvm.heapmemory.used.request"), new HeapMemory());
		timer.schedule(new MonitoringRequestFactory("traktor.local.jvm.heapmemory.used",  workers) ,10, TimeUnit.SECONDS);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}


}

class MonitoringRequestFactory implements Consumer<Long> {

	private final String name;
	private final EventBus workers;
	
	public MonitoringRequestFactory(String name, EventBus workers) {
		super();
		this.name = name;
		this.workers = workers;
	}

	@Override
	public void accept(Long t) {
		workers.notify(name + ".request", Event.wrap(t, name + ".results"));
	}
	
}