package org.traktor.domain.sampling;

import java.io.Serializable;

import reactor.bus.Event;
import reactor.fn.Consumer;
import reactor.fn.Pausable;

public class Sampling<T> implements Serializable, Consumer<Event<T>>{

	private static final long serialVersionUID = 1840597639832443897L;
	
	private final String name;
	private T lastValue;
	
	public Sampling(String name, Pausable pausable) {
		super();
		this.name = name;
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

	@Override
	public void accept(Event<T> monitoringEvent) {
		this.lastValue = monitoringEvent.getData();
	}

	public String getName() {
		return name;
	}

	public T getLastValue() {
		return lastValue;
	}
	
	
}
