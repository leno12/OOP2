package at.tugraz.oo2.data;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents one data point in a data series. The meaning of the
 * field <b>value</b> is defined in the class <b>DataSeries</b>.
 * <p>
 * For example, the meaning (metric) of the field <b>value</b> can be <b>°C</b>
 * for temperature, <b>%</b> for relative humidity, and more.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class DataPoint implements Serializable {
	private final long time;
	private final double value;
}
