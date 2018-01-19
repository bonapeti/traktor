package org.traktor.domain.riemann;

import java.io.IOException;
import java.util.function.Consumer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.traktor.domain.Observation;

import io.riemann.riemann.client.RiemannClient;

@Component
public class Riemann implements Consumer<Observation>, InitializingBean, DisposableBean{

	RiemannClient riemannClient;
	
	@Value("${riemann.host}")
	String host;
	
	@Value("${riemann.port}")
	int port;
	
	@Override
	public void accept(Observation o) {
		try {
			riemannClient.event().
			  service(o.getName()).
			  state("running").
			  metric(5.3).
			  tags("appliance", "cold").
			  send().
			  deref(5000, java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() throws Exception {
		riemannClient.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		riemannClient = RiemannClient.tcp(host, port);
	}

}
