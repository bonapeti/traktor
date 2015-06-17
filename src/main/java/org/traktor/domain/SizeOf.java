package org.traktor.domain;

import java.util.Map;

import reactor.fn.Supplier;

public class SizeOf implements Supplier<Integer> {

	private final Map<? extends Object, ? extends Object> collection;

	@Override
	public Integer get() {
		return collection.size();
	}

	public SizeOf(Map<? extends Object, ? extends Object> collection) {
		this.collection = collection;
	}
	
	
}
