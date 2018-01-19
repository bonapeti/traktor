package org.traktor.domain;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Observation {
	
	private final String name;
	private final Object value;
	@JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
	private final Instant time;
	private final Duration duration;
	
	public Observation(String name, Object value, Instant time, Duration duration) {
		super();
		this.name = name;
		this.value = value;
		this.time = time;
		this.duration = duration;
	}

	public String getName() {
		return name;
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

	@Override
	public String toString() {
		return "Observation [value=" + value + ", type=" + value.getClass().getCanonicalName() + ", time=" + time + ", duration=" + duration + "]";
	}
	
	
}
