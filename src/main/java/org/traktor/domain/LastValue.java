package org.traktor.domain;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LastValue implements Serializable, Consumer<Observation>{

	private static final long serialVersionUID = 1840597639832443897L;
	
	private final String name;
	private AtomicReference<Observation> lastObservation = new AtomicReference<>(null);
	
	public LastValue(String name) {
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
		LastValue other = (LastValue) obj;
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

}
