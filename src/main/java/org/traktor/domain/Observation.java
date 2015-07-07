package org.traktor.domain;

import java.util.Date;

public class Observation {

	private final Object value;
	private final Date time;
	private final long duration;
	
	public Observation(Object value, Date time, long duration) {
		super();
		this.value = value;
		this.time = time;
		this.duration = duration;
	}

	public Object getValue() {
		return value;
	}

	public Date getTime() {
		return time;
	}

	public long getDuration() {
		return duration;
	}
	
	
}
