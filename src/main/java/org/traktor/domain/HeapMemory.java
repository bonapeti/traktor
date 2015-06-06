package org.traktor.domain;

import reactor.bus.Event;
import reactor.fn.Consumer;

public class HeapMemory implements Consumer<Event<Long>> {

	@Override
	public void accept(Event<Long> t) {
		System.out.println(Thread.currentThread().getName() + " " + t);
	}

}
