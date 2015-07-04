package org.traktor.domain;

import java.util.Date;

public class Observation {

	private final Object value;
	private final Date time;
	
	public Observation(Object value, Date time) {
		super();
		this.value = value;
		this.time = time;
	}

	public Object getValue() {
		return value;
	}

	public Date getTime() {
		return time;
	}
	
	
}
