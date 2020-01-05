package at.tugraz.oo2.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * Holds the combination of a location and a metric of a sensor.
 * <p>
 * One "real" sensor measures many metrics (e.g. pressure, humidity, and temperature),
 * but this class only represents one combination of location and metric. A "real" sensor
 * that measures three metrics is represented with three instances of this class.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class Sensor implements Serializable {

	/**
	 * These Strings must hold the exact same content as provided by InfluxDB.
	 */
	private final String location, metric;


	public String prettyString() {
		return String.format("%s - %s", getLocation(), getMetric());
	}


}
