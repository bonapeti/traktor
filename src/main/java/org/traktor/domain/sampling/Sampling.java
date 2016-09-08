package org.traktor.domain.sampling;

import java.io.Serializable;

import org.traktor.domain.Observation;


import reactor.core.Cancellation;

public class Sampling<T> implements Serializable{

	private static final long serialVersionUID = 1840597639832443897L;
	
	private final String name;
	private Observation lastObservation;
	private final Cancellation cancellation;
	
	public Sampling(String name, Cancellation cancellation) {
		super();
		this.name = name;
		this.cancellation = cancellation;
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
		return lastObservation;
	}
	
	
}
