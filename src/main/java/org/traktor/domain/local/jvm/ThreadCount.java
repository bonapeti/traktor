package org.traktor.domain.local.jvm;

import java.lang.management.ManagementFactory;

import reactor.fn.Supplier;

public class ThreadCount implements Supplier<Integer> {

	@Override
	public Integer get() {
		return ManagementFactory.getThreadMXBean().getThreadCount();
	}
}
