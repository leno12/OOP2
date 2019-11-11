package at.tugraz.oo2.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.MathArrays;

import at.tugraz.oo2.Util;

public final class DataSeries implements Serializable, Iterable<DataPoint> {
	private final long minTime, maxTime, interval;
	private final double[] data;
	private final boolean[] present;


	public DataSeries(long minTime, long maxTime,long interval, double[] data, boolean[] present) {
		if (data.length == 0 || interval <= 0 || data.length != present.length) {
			throw new IllegalArgumentException();
		}
		this.minTime = minTime;
		this.maxTime = maxTime;
		this.interval = interval;
		this.data = data;
		this.present = present;
	}

	/**
	 * The start of this data series. Inclusive.
	 *
	 * @return
	 */
	public long getMinTime() {
		return minTime;
	}

	/**
	 * The end of this data series. Exclusive.
	 */
	public long getMaxTime() {
		return this.maxTime;
	}

	public long getInterval() {
		return interval;
	}

	/**
	 * Returns the timespan covered by this series, NOT the number of data points present.
	 */
	public long getLength() {
		return interval * data.length;
	}

	public double[] getData() {
		return data;
	}

	public boolean[] getPresent() {
		return present;
	}

	public int getValueCount() {
		return data.length;
	}

	/**
	 * Returns the number of data points in this series.
	 */
	public int getPresentValueCount() {
		int count = 0;
		for (final boolean b : present) {
			if (b) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Returns the value for a specific time. Returns null if there is no value present or the given timestamp
	 * lies outside the range of this time series.
	 */
	public Double getValue(long time) {
		final int index = (int) ((time - minTime) / interval);
		try {
			return present[index] ? data[index] : null;
		} catch (final ArrayIndexOutOfBoundsException ex) {
			return null;
		}
	}

	public List<DataPoint> getDataPoints() {
		final List<DataPoint> list = new ArrayList<>();
		long time = minTime;
		for (int i = 0; i < data.length; i++) {
			if (present[i]) {
				list.add(new DataPoint(time, data[i]));
			}
			time += interval;
		}
		return list;
	}

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		str.append("DataSeries{from=" + Util.TIME_FORMAT.format(minTime) + " to=" + Util.TIME_FORMAT.format(getMaxTime()) + " values[" + getValueCount() + "]=[");
		for (final DataPoint point : this) {
			str.append('\n' + Util.TIME_FORMAT.format(new Date(point.getTime())) + ": " + point.getValue());
		}
		str.append("\n]}");
		return str.toString();
	}

	@Override
	public Iterator<DataPoint> iterator() {
		return new Iterator<>() {
			private int i = 0;
			private long time = minTime;

			@Override
			public boolean hasNext() {
				while (i < data.length) {
					if (present[i]) {
						return true;
					}
					i++;
					time += interval;
				}
				return false;
			}

			@Override
			public DataPoint next() {
				final DataPoint point = new DataPoint(time, data[i]);
				i++;
				time += interval;
				return point;
			}
		};
	}

	/**
	 * Is true if there are values absent from this time series.
	 */
	public boolean hasGaps() {
		for (final boolean b : present) {
			if (!b) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a new data series containing a segment of this data series.
	 * 
	 * @param startTime Inclusive.
	 * @param endTime   Exclusive.
	 */
	public DataSeries subSeries(long startTime, long endTime) {
		final int start = (int) ((startTime - minTime) / interval);
		final int end = (int) ((endTime - minTime) / interval);
		final double[] dataCopy = Arrays.copyOfRange(data, start, end);
		final boolean[] presentCopy = Arrays.copyOfRange(present, start, end);
		return new DataSeries(minTime + start * interval, maxTime,interval, dataCopy, presentCopy);
	}

	/**
	 * Scales this data series to a new interval. The bigger the inverval, the less data points the time series will have.
	 * This function only works if the new interval is divisible by the current interval. You can come up with something
	 * better if you want.
	 */
	public DataSeries scale(long newInterval) {
		if (newInterval < interval) {
			throw new IllegalArgumentException("Cannot upsample data, unless you implement it. (You likely don't need that.)");
		}
		if (newInterval == interval) {
			return this;
		}
		if (newInterval % interval != 0) {
			throw new IllegalArgumentException("New interval " + newInterval + " must be divisible by current interval " + interval + ".");
		}
		if (newInterval > getLength()) {
			throw new IllegalArgumentException("New interval is bigger than whole series.");
		}
		final int windowSize = (int) (newInterval / interval);
		final double[] newData = new double[data.length / windowSize];
		final boolean[] newPresent = new boolean[newData.length];
		for (int i = 0; i < newData.length; i++) {
			double sum = 0;
			int count = 0;
			final int to = (i + 1) * windowSize;
			for (int from = i * windowSize; from < to; from++) {
				if (present[from]) {
					sum += data[from];
					count++;
				}
			}
			// if (count > windowSize / 2) {
			if (count != 0) {
				newData[i] = sum / count;
				newPresent[i] = true;
			}
		}
		return new DataSeries(minTime, maxTime, newInterval, newData, newPresent);
	}

	/**
	 * Returns a copy of this data series where the range of values is scaled between 0 and 1. If all
	 * values are identical, all values in the returned series will be 0.5.
	 */
	public DataSeries normalize() {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		boolean none = true;
		for (int i = 0; i < data.length; i++) {
			if (present[i]) {
				final double value = data[i];
				if (value < min) {
					min = value;
				}
				if (value > max) {
					max = value;
				}
				none = false;
			}
		}
		if (none) {
			return this;
		}
		final double[] normalizedData = new double[data.length];
		if (min == max) {
			Arrays.fill(normalizedData, 0.5);
		} else {
			final double gap = max - min;
			for (int i = 0; i < data.length; i++) {
				if (present[i]) {
					normalizedData[i] = (data[i] - min) / gap;
				}
			}
		}
		return new DataSeries(minTime,maxTime, interval, normalizedData, present);
	}

	/**
	 * Scales all values in this array to fit between 0 and 1. Fills the array with 0.5 if
	 * all values are identical.
	 */
	public static void normalize(double[] array) {
		if (array.length == 0) {
			return;
		}
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (final double value : array) {
			if (value < min) {
				min = value;
			}
			if (value > max) {
				max = value;
			}
		}
		if (min == max) {
			Arrays.fill(array, 0.5);
			return;
		}
		final double gap = max - min;
		for (int i = 0; i < array.length; i++) {
			array[i] = (array[i] - min) / gap;
		}
	}

	/**
	 * Attempts to interpolate missing data points and returns the result as a new data series.
	 */
	public DataSeries interpolate() {
		final double[] resultData = data.clone();
		final boolean[] resultPresent = present.clone();
		for (int i = resultData.length - 2; i >= 1; i--) {
			if (!present[i] && present[i - 1] && present[i + 1]) {
				resultData[i] = (data[i - 1] + data[i + 1]) / 2;
				resultPresent[i] = true;
			}
		}
		return new DataSeries(minTime,maxTime, interval, resultData, resultPresent);
	}

	/**
	 * Calculates the similarity between this time series and the given array. Both must have the same
	 * length and must not have gaps. Returns the euclidean distance between these two.
	 */
	public double similarity(double[] ref) {
		if (hasGaps()) {
			throw new IllegalArgumentException("Series must have no gaps!");
		}
		return MathArrays.distance(data, ref);
	}
}
