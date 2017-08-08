package org.traktor.domain;

@FunctionalInterface
public interface Sampler<T> {

	T takeSample() throws Exception;
}
