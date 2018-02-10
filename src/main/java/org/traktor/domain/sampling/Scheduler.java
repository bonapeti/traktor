package org.traktor.domain.sampling;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mpierce.metrics.reservoir.hdrhistogram.HdrHistogramReservoir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.traktor.domain.Observation;
import org.traktor.domain.Request;
import org.traktor.domain.Sampler;
import org.traktor.domain.Sampling;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.TopicProcessor;
import reactor.core.scheduler.Schedulers;


@RestController
public class Scheduler {

	
	@Autowired
	private TopicProcessor<Observation> resultTopic;
	
	
	@Autowired
	private Meter monitoringRequests;
	
	@Autowired
	private Meter monitoringErrors;
	
	@Autowired
	private MetricRegistry metrics;
	
	private Set<Sampling> samplings = Collections.newSetFromMap(new ConcurrentHashMap<Sampling,Boolean>());
	
	@RequestMapping(value="/sampling", method=RequestMethod.GET)
    public Collection<Sampling> samplings() {
        return samplings;
    }
	
	@RequestMapping(value="/sampling/{name}", method=RequestMethod.GET)
    public Sampling sampling(@PathVariable String name) {
		for (Sampling sampling : samplings) {
			if (name.equals(sampling.getName())) {
				return sampling;
			}
		}
        throw new IllegalStateException("No sampling with name '" + name + "' found");
    }
	
	@RequestMapping(value="/sampling/{name}/last", method=RequestMethod.GET)
    public Observation lastObservation(@PathVariable String name) {
		return sampling(name).getLast();
    }
	
	@RequestMapping(value="/sampling/{name}/latency", method=RequestMethod.GET)
    public Snapshot latency(@PathVariable String name) {
		return sampling(name).getLatency();
    }
	
	@RequestMapping(value="/sampling/{name}/last/value", method=RequestMethod.GET)
    public Object lastValue(@PathVariable String name) {
		return lastObservation(name).getValue();
    }
	
	public int size() {
		return samplings.size();
	}
	
	
	public <T> void schedule(final String name, final Sampler<T> sampler, long secondPeriod) {

		
		Timer timer = metrics.timer(name + ".latency", new MetricSupplier<Timer>() {
			
			@Override
			public Timer newMetric() {
				return new Timer(new HdrHistogramReservoir());
			}
		});
		Meter errorMeter = metrics.meter(name + ".errors");
		
		
		Flux<Request<T>> requests = Flux.interval(Duration.ZERO, Duration.ofSeconds(secondPeriod))
				.map((time) ->  new Request<T>(Instant.now(), sampler))
				.publishOn(Schedulers.elastic())
				.log();
		
		ConnectableFlux<Observation> observations = requests.map((request) -> {

			Instant when = Instant.now();
			Timer.Context timerContext = timer.time();
			
			try {
				T value = sampler.takeSample();
				return new Observation(name, value, when, Duration.ofNanos(timerContext.stop()));
			} catch (Exception e) {
				errorMeter.mark();
				return new Observation(name, e.getClass().getName() + ": " + e.getMessage(), when, null);
			} 
		})
				//.publishOn(Schedulers.elastic())
				.log().publish();
		
		
		Sampling sampling = new Sampling(name, timer, requests, observations);
		
		observations.subscribe(resultTopic);
		observations.subscribe(sampling);
		
		
		samplings.add(sampling);
		
		observations.connect();
	}
	
}
