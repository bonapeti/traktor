package org.traktor.domain;

import java.time.Instant;
import java.util.function.Supplier;

public class Request<R> {

	private final Instant when;
	private final Supplier<R> supplier;
	
	public Request(Instant when, Supplier<R> supplier) {
		super();
		this.when = when;
		this.supplier = supplier;
	}

	@Override
	public String toString() {
		return "Request [when=" + when + ", supplier=" + supplier + "]";
	}
	
	
}
