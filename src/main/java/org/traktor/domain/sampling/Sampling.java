package org.traktor.domain.sampling;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.traktor.domain.Observation;
import org.traktor.domain.Request;

import reactor.core.publisher.Flux;

public class Sampling<T> implements Serializable, Consumer<Observation>{

	private static final long serialVersionUID = 1840597639832443897L;
	
	private final String name;
	private final Flux<Request<T>> requests;
	private AtomicReference<Observation> lastObservation = new AtomicReference<Observation>(null);
	
	public Sampling(String name, Flux<Request<T>> requests, Flux<Observation> lastObservation) {
		super();
		this.name = name;
		this.requests = requests;
		lastObservation.subscribe(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sampling<?> other = (Sampling<?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String getName() {
		return name;
	}

	public Observation getLastObservation() {
		return lastObservation.get();
	}

	@Override
	public void accept(Observation t) {
		lastObservation.set(t);
	}
	
	public void start() {
		requests.subscribe();
	}
}
