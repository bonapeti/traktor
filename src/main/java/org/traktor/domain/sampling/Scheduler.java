package org.traktor.domain.sampling;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.traktor.domain.Observation;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;
import reactor.fn.Supplier;
import reactor.fn.timer.Timer;

@RestController
public class Scheduler {

	@Autowired
	private EventBus eventBus;
	
	@Autowired
	private EventBus workers;
	
	@Autowired
	private Timer timer;

	private Set<Sampling<?>> samplings = Collections.newSetFromMap(new ConcurrentHashMap<Sampling<?>,Boolean>());
	
	@RequestMapping(value="/sampling", method=RequestMethod.GET)
    public Collection<Sampling<?>> samplings() {
        return samplings;
    }
	
	@RequestMapping(value="/sampling/{name}", method=RequestMethod.GET)
    public Sampling<?> sampling(@PathVariable String name) {
		for (Sampling<?> sampling : samplings) {
			if (name.equals(sampling.getName())) {
				return sampling;
			}
		}
        throw new IllegalStateException("No sampling with name '" + name + "' found");
    }
	
	@RequestMapping(value="/sampling/{name}/last", method=RequestMethod.GET)
    public Observation lastObservation(@PathVariable String name) {
		return sampling(name).getLastObservation();
    }
	
	@RequestMapping(value="/sampling/{name}/last/value", method=RequestMethod.GET)
    public Object lastValue(@PathVariable String name) {
		return lastObservation(name).getValue();
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