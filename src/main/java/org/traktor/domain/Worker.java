package org.traktor.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Meter;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import reactor.fn.Supplier;

@Component
public class Worker implements Consumer<Event<Supplier<Object>>> {

	@Autowired
	private EventBus eventBus;
	
	@Autowired
	private Meter monitoringRequests;
	
	@Override
	public void accept(Event<Supplier<Object>> t) {
		monitoringRequests.mark();
		eventBus.notify(t.getReplyTo(), Event.wrap(t.getData().get()));
	}

}
