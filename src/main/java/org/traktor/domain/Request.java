package org.traktor.domain;

import java.time.Instant;

public class Request<R> {

	private final Instant when;
	private final Sampler<R> sampler;
	
	public Request(Instant when, Sampler<R> sampler) {
		super();
		this.when = when;
		this.sampler = sampler;
	}

	@Override
	public String toString() {
		return "Request [when=" + when + ", sampler=" + sampler + "]";
	}
	
	
}
