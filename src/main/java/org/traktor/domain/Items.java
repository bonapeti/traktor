package org.traktor.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.traktor.Item;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;
import reactor.fn.Supplier;
import reactor.fn.timer.Timer;

@Component
public class Items {

	@Autowired
	private EventBus eventBus;
	
	@Autowired
	private EventBus workers;
	
	@Autowired
	private Timer timer;

	private Set<Item<?>> items = Collections.newSetFromMap(new ConcurrentHashMap<Item<?>,Boolean>());
	
	public int size() {
		return items.size();
	}

	public Collection<Item<?>> getNames() {
		return items;
	}
	
	public <T> void addItem(String name, Supplier<T> supplier, long secondPeriod) {
		Item<T> item = new Item<T>(name, timer.schedule(new MonitoringRequestFactory<T>(supplier, name, workers) , secondPeriod, TimeUnit.SECONDS));
		eventBus.on(Selectors.$(name + ".results"), item);
		items.add(item);
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