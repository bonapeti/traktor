package org.traktor.domain;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

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
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		Object value = t.getData().get();
		stopWatch.stop();
		eventBus.notify(t.getReplyTo(), Event.wrap(new Observation(value, new Date(), stopWatch.getLastTaskTimeMillis())));
	}

}
