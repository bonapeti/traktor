package org.traktor.domain.sampling;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.traktor.domain.Observation;
import org.traktor.domain.Request;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;


@RestController
public class Scheduler {

	private MetricRegistry metrics = new MetricRegistry();
	
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
	
	public <T> void schedule(String name, final Supplier<T> supplier, long secondPeriod) {

		
		Timer timer = metrics.timer(name);
		
		Flux<Request<T>> requests = Flux.intervalMillis(0l, 1000l * secondPeriod)
				.map((time) ->  new Request<T>(Instant.now(), supplier))
				.publishOn(Schedulers.parallel());
		
		Flux<Observation> observations = requests.map((request) -> {
			Instant when = Instant.now();
			Timer.Context timerContext = timer.time();
			Object value = supplier.get();
			long measurementDuration = timerContext.stop();
			return new Observation(value, when, Duration.ofNanos(measurementDuration));
		}).log();
		
		Sampling<T> sampling = new Sampling<T>(name, requests, observations);
		
		sampling.start();
		
		samplings.add(sampling);
	}
	
}

class AlarmClock<T> implements Consumer<Request<T>> {

	private final Supplier<T> item;
	private final String name;
	
	public AlarmClock(Supplier<T> item, String name) {
		super();
		this.item = item;
		this.name = name;
	}
	
	@Override
	public void accept(Request<T> t) {
		T value = item.get();
		//System.out.println("name: " + name + ",Thread: " + Thread.currentThread().getName() + ", value: " + value.toString());
		
	}
	
}