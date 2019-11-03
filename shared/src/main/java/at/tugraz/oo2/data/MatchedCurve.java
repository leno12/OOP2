package at.tugraz.oo2.data;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a single match returned by similarity search.
 */
@Data
@AllArgsConstructor
public final class MatchedCurve implements Serializable {
	private final Sensor sensor;
	private final DataSeries series;
	private final double error;
}
