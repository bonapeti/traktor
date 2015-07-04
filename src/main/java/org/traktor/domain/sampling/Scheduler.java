package org.traktor.domain.sampling;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;
import reactor.fn.Supplier;
import reactor.fn.timer.Timer;

@Component
@RestController
public class Scheduler {

	@Autowired
	private EventBus eventBus;
	
	@Autowired
	private EventBus workers;
	
	@Autowired
	private Timer timer;

	private Set<Sampling<?>> samplings = Collections.newSetFromMap(new ConcurrentHashMap<Sampling<?>,Boolean>());
	
	@RequestMapping(method=RequestMethod.GET)
    public Collection<Sampling<?>> sampling() {
        return samplings;
    }
	
	public int size() {
		return samplings.size();
	}
	
	public <T> void schedule(String name, Supplier<T> supplier, long secondPeriod) {
		Sampling<T> sampling = new Sampling<T>(name, timer.schedule(new AlarmClock<T>(supplier, name, workers) , secondPeriod, TimeUnit.SECONDS));
		eventBus.on(Selectors.$(name + ".results"), sampling);
		samplings.add(sampling);
	}
	
}

class AlarmClock<T> implements Consumer<Long> {

	private final Supplier<T> item;
	private final String name;
	private final EventBus workers;
	
	public AlarmClock(Supplier<T> item, String name, EventBus workers) {
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