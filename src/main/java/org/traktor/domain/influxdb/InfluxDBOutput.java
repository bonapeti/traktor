package org.traktor.domain.influxdb;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.LogLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.traktor.domain.Observation;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import okhttp3.OkHttpClient;

@Component
public class InfluxDBOutput implements Consumer<Observation>, InitializingBean, DisposableBean {

	private InfluxDB influxDB;
	
	@Value("${influxdb.host}")
	String host;
	
	@Value("${influxdb.port}")
	int port;
	
	@Autowired
	private MetricRegistry metrics;
	
	public InfluxDBOutput() {
		
	}
	
	public InfluxDBOutput(String host, int port, MetricRegistry metricRegistry) {
		this.host = host;
		this.port = port;
		this.metrics = metricRegistry;
	}
	
	private Meter errorMeter;
	
	private volatile boolean shuttingDown = false;
	
	private static Logger logger = Logger.getLogger(InfluxDBOutput.class);
	
	
	
	@Override
	public void accept(Observation t) {
		
		Point point;
		Point.Builder pointBuilder = Point.measurement(t.getName())
				.time(t.getTime().toEpochMilli(), TimeUnit.MILLISECONDS);
				//.addField("duration", t.getDuration().toMillis());
		if (t.getValue() instanceof Number) {
			pointBuilder = pointBuilder.addField("value", (Number)t.getValue());
		} else if (t.getValue() instanceof String) {
			pointBuilder = pointBuilder.addField("value", (String)t.getValue());
		} else if (t.getValue() instanceof Boolean) {
			pointBuilder = pointBuilder.addField("value", (Boolean)t.getValue());
		}
		
			try  {
				influxDB.write(pointBuilder.build());
			} catch (Exception e) {
				errorMeter.mark();
				logger.warn("Failed to connect to InfluxDB", e);
			}	
		
		
	}

	@Override
	public void destroy() throws Exception {
		influxDB.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		errorMeter = metrics.meter("influxDB.error");
		try {
			OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS);
			influxDB = InfluxDBFactory.connect("http://" + host + ":" + port,"root", "root", httpClientBuilder);
			influxDB.setLogLevel(LogLevel.FULL);
			influxDB.setDatabase("mydb");
		} catch (Exception ex) {
			logger.warn("Failed to connect to InfluxDB", ex);
		}
	}


}
