package org.traktor.domain.local.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import reactor.fn.Supplier;


public class NonHeapMemory implements Supplier<MemoryUsage> {

	@Override
	public MemoryUsage get() {
		return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
	}

}