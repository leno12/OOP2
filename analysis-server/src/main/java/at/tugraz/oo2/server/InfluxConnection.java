package at.tugraz.oo2.server;

import at.tugraz.oo2.Util;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;

import com.google.gson.*;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


import java.util.*;

/**
 * Handles the Influx API and should provide features like querying sensors and their values, etc.
 */
public final class InfluxConnection {

	private final String url;
	private final String databaseName;
	private final String userName;
	private final String password;

	public InfluxConnection(String url, String databaseName, String userName, String password) {
		this.url = url;
		this.databaseName = databaseName;
		this.userName = userName;
		this.password = password;
	}


	/**
	 * 	Get list of all locations and metrics available in the influxdb
	 * @return List of sensors
	 * @throws UnirestException
	 */
	public List<Sensor> getLocationandMetrics() throws UnirestException {

		List<Sensor> ls = new ArrayList<>();
		String request = Unirest
				.get(url + '/' + "query?db=" + databaseName + "&u=" + userName + "&p=" + password +
						"&q=SHOW%20SERIES%20on%20oo2")
				.asString()
				.getBody();

		JsonArray sensors = this.getJsonArrayWithValues(request);
		List<Sensor> sensor_list = new ArrayList<>();
		for(int i = 0; i  < sensors.size(); i++)
		{
			String[] tokens = sensors.get(i).getAsString().split(",");
			String[] tokens2 = tokens[1].split("=");
			Sensor sensor = new Sensor(tokens2[1], tokens[0]);
			sensor_list.add(sensor);
		}

		return sensor_list;

	}


	/**
	 * Get the latest value from influxdb for the wanted sensor
	 * @param sensor
	 * @return DataPoint
	 * @throws UnirestException
	 */
	public DataPoint getCurrentSensorValue(Sensor sensor) throws UnirestException {
		String location = sensor.getLocation();
		String metric = sensor.getMetric();
		String request = Unirest
				.get(url + '/' + "query?db=" + databaseName + "&u=" + userName + "&p=" + password +
						"&q=SELECT%20*%20FROM%20" + metric +"%20WHERE%20location%20=%20%27" + location +
						"%27%20ORDER%20BY%20time%20DESC%20LIMIT%201")
				.asString()
				.getBody();

		try {
			JsonArray sensors = this.getJsonArrayWithValues(request);
			JsonArray current_sensor_value = sensors.get(0).getAsJsonArray();
			String time_string = current_sensor_value.get(0).getAsString();
			DateTime dateTimeLocalzone = new DateTime(time_string, DateTimeZone.getDefault());
			Date date = dateTimeLocalzone.toDate();
			long time = date.getTime();
			double value = current_sensor_value.get(2).getAsDouble();
			DataPoint current_value = new DataPoint(time,value);
			return current_value;
		}
		catch (NullPointerException e)
		{
			System.out.println("Sensor does not exist");
			e.printStackTrace();
			return null;
		}



	}


	/**
	 * 	Get data series from the Influxdb for the wanted sensor, time range and interval
	 * @param sensor
	 * @param from
	 * @param to
	 * @param interval
	 * @param cache
	 * @return DataSeries
	 * @throws UnirestException
	 */
	public DataSeries getDataSeries(Sensor sensor, long from, long to, long interval, Cache cache)  {
		String location = sensor.getLocation();
		String metric = sensor.getMetric();
		long cache_interval = Util.EPOCH;
		long time_offset = from/cache_interval;
		time_offset = time_offset*cache_interval;
		time_offset = from - time_offset;
		String request = null;
		String query = "&q=SELECT%20FIRST(*)%20FROM%20" + metric + "%20WHERE%20location%20=%20%27" + location +
				"%27%20and%20time%3E%3D" + from + "ms%20and%20time%3C" + to +
				"ms%20GROUP%20BY%20time(" + cache_interval + "ms," + time_offset + "ms)%20fill(0)";
		try {
			 request = Unirest
					.get(url + '/' + "query?db=" + databaseName + "&u=" + userName + "&p=" + password + query)
					.asString()
					.getBody();
		}catch (UnirestException e)
		{
			System.out.println("unirest");
		}
		try {
			JsonArray values = this.getJsonArrayWithValues(request);

			double data[] = new double[values.size()];
			boolean present[] = new boolean[values.size()];

			for(int i = 0; i < values.size(); i++) {
				double current_sensor_value = values.get(i).getAsJsonArray().get(1).getAsDouble();
				if (current_sensor_value != 0.0) {
					present[i] = true;
				} else {
					present[i] = false;
				}
				data[i] = current_sensor_value;

			}

			DataSeries data_series = new DataSeries(from,to, cache_interval, data, present);
			cache.insertNewEntry(location, metric, data_series, false);
			data_series = data_series.scale(interval);

			return data_series;
		}catch (NullPointerException e)
		{
			System.out.println("Sensor does not exist");
			return null;
		}

	}

	/**
	 * 	This Method is used to parse json that we get from the api using Unirest
	 * 	Returns JsonArray containing needed values
	 * @param request
	 * @return JsonArray
	 */

	public JsonArray getJsonArrayWithValues(String request)
	{
		JsonElement jElement = new JsonParser().parse(request);

		JsonObject jObject = jElement.getAsJsonObject();
		JsonArray resultsJArray = jObject.getAsJsonArray("results");
		JsonObject jsonObject = resultsJArray.get(0).getAsJsonObject();
		JsonArray seriesJsonArray = jsonObject.get("series").getAsJsonArray();
		JsonObject seriesJObject = seriesJsonArray.get(0).getAsJsonObject();
		JsonArray values = seriesJObject.get("values").getAsJsonArray();
		return  values;

	}
}
