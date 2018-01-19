package org.traktor.domain.influxdb;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.LogLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.traktor.domain.Observation;

@Component
public class InfluxDBOutput implements Consumer<Observation>, InitializingBean, DisposableBean {

	private InfluxDB influxDB;
	
	@Value("${influxdb.host}")
	String host;
	
	@Value("${influxdb.port}")
	int port;
	
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
		
		influxDB.write(pointBuilder.build());
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		influxDB = InfluxDBFactory.connect("http://" + host + ":" + port,"root", "root");
		influxDB.setLogLevel(LogLevel.FULL);
		influxDB.setDatabase("mydb");
		influxDB.createRetentionPolicy("autogen", "mydb", "30d", "30m", 2, true);
	}

}
