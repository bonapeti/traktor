package org.traktor.domain;

import java.time.Duration;
import java.time.Instant;

public class Observation {
	
	private final Object value;
	private final Instant time;
	private final Duration duration;
	
	public Observation(Object value, Instant time, Duration duration) {
		super();
		this.value = value;
		this.time = time;
		this.duration = duration;
	}

	public Object getValue() {
		return value;
	}

	public Instant getTime() {
		return time;
	}

	public Duration getDuration() {
		return duration;
	}
}
