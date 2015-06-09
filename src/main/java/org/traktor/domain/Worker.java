package org.traktor.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import reactor.fn.Supplier;

@Component
public class Worker implements Consumer<Event<Supplier<Object>>> {

	@Autowired
	private EventBus eventBus;
	
	@Override
	public void accept(Event<Supplier<Object>> t) {
		eventBus.notify(t.getReplyTo(), Event.wrap(t.getData().get()));
	}

}
