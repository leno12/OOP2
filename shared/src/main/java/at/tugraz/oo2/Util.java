package at.tugraz.oo2;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Holds some constants and useful methods.
 */
public final class Util {
	public static final long EPOCH = 5 * 60 * 1000;

	public static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final DateFormat USER_TIME_FORMAT0 = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
	private static final DateFormat USER_TIME_FORMAT1 = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");

	public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	public static final DateFormat INFLUX_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static final DateFormat HUMAN_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static final DateFormat HUMAN_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

	static {
		INFLUX_TIME_FORMAT.setTimeZone(UTC);
	}

	public static long parseUserTime(String in) {
		try {
			return USER_TIME_FORMAT1.parse(in).getTime();
		} catch (final ParseException ex) {

		}
		try {
			return USER_TIME_FORMAT0.parse(in).getTime();
		} catch (final ParseException ex) {

		}
		try {
			return DATE_FORMAT.parse(in).getTime();
		} catch (final ParseException ex) {

		}
		throw new IllegalArgumentException("Invalid date format " + in);
	}

	/**
	 * Resamples an array containing a time series to a new length. For example,
	 * if the input is {1, 2, 3} and the array is scaled to 6 elements, the
	 * output will be {1, 1, 2, 2, 3, 3}.
	 */
	public static double[] resize(double[] in, int outLength) {
		final int inLength = in.length;
		if (inLength == outLength) {
			return in;
		}
		final double[] out = new double[outLength];
		final double factor = inLength / (double) outLength;
		for (int i = 0; i < outLength; i++) {
			final int j = (int) (i * factor);
			out[i] = in[j];
		}
		return out;
	}

	public static double[] interpolate(double[] data) {
		int startIdx = -1;
		double startValue = 0f;
		double element;
		for (int i = 0; i < data.length - 1; i++) {
			element = data[i];
			if (element != 0f) {
				if (startIdx != -1) {
					doInterpolate(startValue, element, startIdx + 1, i - startIdx - 1, data);
				}
				startValue = element;
				startIdx = i;
			}
		}
		return data;
	}

	private static void doInterpolate(double start, double end, int startIdx, int count, double[] data) {
		double delta = (end - start) / (count + 1);
		for (int i = startIdx; i < startIdx + count; i++) {
			data[i] = start + delta * (i - startIdx + 1);
		}
	}

	private Util() {

	}
}
