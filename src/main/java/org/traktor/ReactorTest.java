package org.traktor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDB.LogLevel;
import org.influxdb.dto.Point;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.traktor.domain.Observation;
import org.traktor.domain.Sampler;
import org.traktor.domain.influxdb.InfluxDBOutput;
import org.traktor.domain.net.OpenSocket;
import org.traktor.domain.riemann.Riemann;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.riemann.riemann.client.RiemannClient;
import okhttp3.OkHttpClient;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.TopicProcessor;
import reactor.core.scheduler.Schedulers;

public class ReactorTest {

	private static Logger logger = Logger.getLogger(ReactorTest.class);

	public static void main(String[] args) throws Exception {

		MetricRegistry metricRegistry = new MetricRegistry();
		
		final RiemannClient riemannClient = RiemannClient.tcp("localhost", 5555);
		
		final OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS);
		final InfluxDB influxDB = InfluxDBFactory.connect("http://" + "localhost" + ":" + 8086, "root", "root",
				httpClientBuilder);
		influxDB.setLogLevel(LogLevel.NONE);
		influxDB.setDatabase("mydb");

		final TopicProcessor<Observation> topicProcessor = TopicProcessor.share("Results", 1024);
		topicProcessor.subscribe(
				(result) -> System.out.println(Thread.currentThread().getName() + " 1st consumer " + result));

		ConnectableFlux<Observation> results = topicProcessor.publishOn(Schedulers.elastic()).publish();
		results.subscribe(new InfluxDBConsumer(influxDB));
		results.subscribe(new RiemannConsumer(riemannClient));
		
		results.connect();

		final Sampler<Boolean> sampler = new OpenSocket(new InetSocketAddress("ftp.msci.com", 21), 1000);
		final Timer timer = metricRegistry.timer("ftp.msci.com");
		
		Flux<Observation> flux1 = Flux.interval(Duration.ZERO, Duration.ofSeconds(5)).map((request) -> {

			Instant when = Instant.now();
			Timer.Context timerContext = timer.time();
			
			try {
				Boolean value = sampler.takeSample();
				return new Observation("ftp.msci.com", value, when, Duration.ofNanos(timerContext.stop()));
			} catch (Exception e) {
				
				return new Observation("ftp.msci.com", e.getClass().getName() + ": " + e.getMessage(), when, null);
			} 
		});
		
		flux1.subscribe(topicProcessor);
		
		Thread thread1 = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				}
			}
		}, "Thread1");
		thread1.start();
		thread1.join();
	}

}

class RiemannConsumer implements Consumer<Observation> {

	private static Logger logger = Logger.getLogger(RiemannConsumer.class);

	
	final RiemannClient riemannClient;
	
	
	
	public RiemannConsumer(RiemannClient riemannClient) {
		super();
		this.riemannClient = riemannClient;
	}



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
			logger.info("Failed to connect to Riemann");
		}
	
	}
	
}

class InfluxDBConsumer implements Consumer<Observation> {

	private final InfluxDB influxDB;

	private static Logger logger = Logger.getLogger(InfluxDBConsumer.class);

	public InfluxDBConsumer(InfluxDB influxDB) {
		super();
		this.influxDB = influxDB;
	}

	@Override
	public void accept(Observation t) {
		Point.Builder pointBuilder = Point.measurement(t.getName()).time(t.getTime().toEpochMilli(),
				TimeUnit.MILLISECONDS);
		// .addField("duration", t.getDuration().toMillis());
		if (t.getValue() instanceof Number) {
			pointBuilder = pointBuilder.addField("value", (Number) t.getValue());
		} else if (t.getValue() instanceof String) {
			pointBuilder = pointBuilder.addField("value", (String) t.getValue());
		} else if (t.getValue() instanceof Boolean) {
			pointBuilder = pointBuilder.addField("value", (Boolean) t.getValue());
		}

		try {
			influxDB.write(pointBuilder.build());
		} catch (Exception e) {
			logger.info("Failed to connect to InfluxDB");
		}

	}
}
