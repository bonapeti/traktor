package org.traktor.domain;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import reactor.fn.Pausable;
import reactor.fn.Supplier;
import reactor.fn.timer.Timer;

@Component
public class Items {

	@Autowired
	private EventBus workers;
	
	@Autowired
	private Timer timer;

	private Map<String,Pausable> items = new ConcurrentHashMap<String,Pausable>();
	
	public int size() {
		return items.size();
	}

	public Collection<String> getNames() {
		return items.keySet();
	}
	
	public <T> void addItem(String name, Supplier<T> supplier, long secondPeriod) {
		items.put(name, timer.schedule(new MonitoringRequestFactory<T>(supplier, name, workers) , secondPeriod, TimeUnit.SECONDS));
	}
	
}

class MonitoringRequestFactory<T> implements Consumer<Long> {

	private final Supplier<T> item;
	private final String name;
	private final EventBus workers;
	
	public MonitoringRequestFactory(Supplier<T> item, String name, EventBus workers) {
		super();
		this.item = item;
		this.name = name;
		this.workers = workers;
	}

	@Override
	public void accept(Long t) {
		workers.notify(name + ".requests", Event.wrap(item, name + ".results"));
	}
	
}