package org.traktor.domain.local.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.traktor.domain.Items;

import reactor.fn.Supplier;

@Component
public class NumberOfItems implements Supplier<Integer> {

	@Autowired
	private Items items;

	@Override
	public Integer get() {
		return items.size();
	}
	
}
