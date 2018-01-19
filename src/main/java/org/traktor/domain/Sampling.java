package org.traktor.domain;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

public class Sampling implements Serializable, Consumer<Observation>{

	private static final long serialVersionUID = 1840597639832443897L;
	
	private final String name;
	private AtomicReference<Observation> lastObservation = new AtomicReference<>(null);
	private final Timer latencyTimer;
	
	public Sampling(String name, Timer latencyTimer) {
		super();
		this.name = name;
		this.latencyTimer = latencyTimer;
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
		Sampling other = (Sampling) obj;
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

	public Observation getLast() {
		return lastObservation.get();
	}

	@Override
	public void accept(Observation t) {
		lastObservation.set(t);
	}

	public Snapshot getLatency() {
		return latencyTimer.getSnapshot();
	}
}
