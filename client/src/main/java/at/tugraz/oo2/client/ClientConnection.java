package at.tugraz.oo2.client;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import at.tugraz.oo2.data.ClusterDescriptor;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.MatchedCurve;
import at.tugraz.oo2.data.Sensor;


/**
 * Used for managing the connection to the server and for sending requests.
 */
public final class ClientConnection implements AutoCloseable {

	private LinkedBlockingQueue<ConnectionEventHandler> connectionClosedEventHandlers;
	private LinkedBlockingQueue<ConnectionEventHandler> connectionOpenedEventHandlers;
	private String url;
	private int port;
	private Socket client_socket;
	private ObjectOutputStream out;
	private ObjectInputStream ois;
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_RESET = "\u001B[0m";
	private boolean running = true;
	private boolean maximised = false;

	public boolean getMaximised()
	{
		return this.maximised;
	}
	public void setMaximised(boolean maximised_)
	{
		this.maximised = maximised_;
	}

	public ClientConnection() {
		connectionClosedEventHandlers = new LinkedBlockingQueue<>();
		connectionOpenedEventHandlers = new LinkedBlockingQueue<>();
	}

	public void setRunning(boolean running)
	{
		this.running = running;
	}

	public boolean getRunning()
	{
		return this.running;
	}
	/**
	 * Establishes a connection to the server.
	 */
	public boolean connect(String url, int port) throws IOException {

		try {
			this.client_socket = new Socket(url,port);
			this.client_socket.setSoTimeout(30*1000);
		} catch (IOException e) {
			System.out.println(ANSI_RED + "[ERROR] Can't reach the server" + ANSI_RESET);
			return false;
		}

		this.out = new ObjectOutputStream(this.client_socket.getOutputStream());
		this.ois = new ObjectInputStream(this.client_socket.getInputStream());

		for (ConnectionEventHandler connectionOpenedEventHandler : connectionOpenedEventHandlers)
			connectionOpenedEventHandler.apply();

		return true;

	}

	/**
	 * Registers a handler that will be called when the connection is opened.
	 */
	public void addConnectionClosedListener(ConnectionEventHandler eventHandler) {
		connectionClosedEventHandlers.add(eventHandler);
	}

	/**
	 * Registers a handler that will be called when the connection is closed either by
	 * the client itself or by the server.
	 */
	public void addConnectionOpenedListener(ConnectionEventHandler eventHandler) {
		connectionOpenedEventHandlers.add(eventHandler);
	}

	@Override
	public void close() {
		try {
			for (ConnectionEventHandler connectionClosedEventHandler : connectionClosedEventHandlers)
				connectionClosedEventHandler.apply();
			List<Object> list = new ArrayList<>();
			list.add("exit");
			out.writeObject(list);
			this.client_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	/**
	 * Returns a future holding a list of all known sensors.
	 */
	public CompletableFuture<List<Sensor>> querySensors() throws InterruptedException {

    if(!running)
    	return null;
	try {

		CompletableFuture<List<Sensor>> completableFuture = CompletableFuture.supplyAsync(() ->
		{
			List<Sensor> sensors = new ArrayList<>();


			try {
				List<Object> list = new ArrayList<>();
				list.add("ls");
				out.writeObject(list);
				Object received_object = ois.readObject();
				sensors = (List<Sensor>) received_object;
			}
			catch (SocketTimeoutException e) {
				System.out.println(ANSI_RED + "[ERROR] Socket timeout" + ANSI_RESET);
			}
			catch (IOException e) {
				System.out.println(ANSI_RED + "[ERROR] Can't reach the server - " +
						"check your internet Connection" + ANSI_RESET);
			} catch (ClassNotFoundException e) {
				System.out.println(ANSI_RED + "[ERROR] Class could not be found" + ANSI_RESET);
			}


			return sensors;

		});

		completableFuture.get();
		return completableFuture;
	}
	catch (ExecutionException e)
	{
		System.out.println("Execution exception");
	}
	return null;
	}

	/**
	 * Returns a data point containing the last reading of the given sensor.
	 */
	public CompletableFuture<DataPoint> queryValue(Sensor sensor) {

            if(!running)
            	return null;
			CompletableFuture<DataPoint> completableFuture = CompletableFuture.supplyAsync(() ->
			{
				DataPoint data_point = new DataPoint(0, 0.0);
				try {

					List<Object> list = new ArrayList<>();
					list.add("now");
					list.add(sensor);
					out.writeObject(list);
					Object received_object = ois.readObject();
					data_point = (DataPoint) received_object;

					if(data_point == null)
					{
						data_point = new DataPoint(0,0.0);
						System.out.println(ANSI_RED +  "[ERROR] Wrong parameters" + ANSI_RESET);
					}

					return data_point;

				}
				catch (SocketTimeoutException e) {
					System.out.println(ANSI_RED + "[ERROR] Socket timeout" + ANSI_RESET);
				}
				catch  (IOException  e) {

					System.out.println(ANSI_RED + "[ERROR] Can't reach the server - " +
							"check your internet Connection" + ANSI_RESET);

				}catch (ClassNotFoundException e)
				{
					System.out.println(ANSI_RED + "[ERROR] Class could not be found" + ANSI_RESET);
				}
				return data_point;
			});

		try {
			completableFuture.get();
		} catch (InterruptedException e) {
			System.out.println("Disconnected!");
		} catch (ExecutionException e) {

		}
		return completableFuture;


	}

	/**
	 * Returns a data series containing the queried data.
	 */
	public CompletableFuture<DataSeries> queryData(Sensor sensor, long from, long to, long interval) throws
			ExecutionException,InterruptedException {
			if(!running)
				return null;

		CompletableFuture<DataSeries> completableFuture = CompletableFuture.supplyAsync(() ->
		{
			double[] array = new double[1];
			boolean[] array2 = new boolean[1];

			DataSeries data_series = new DataSeries(0, 1,1, array, array2);
			try {
				if (interval < 1) {
					System.out.println(ANSI_RED + "[ERROR] Wrong interval - interval" +
							           " should be bigger than 0" + ANSI_RESET);
				} else if (from >= to) {
					System.out.println(ANSI_RED + "[ERROR] Wrong date - End time should  " +
							"be greater than Start time" + ANSI_RESET);
				} else if(from >= System.currentTimeMillis() || to >= System.currentTimeMillis()) {
					System.out.println(ANSI_RED + "[ERROR] Wrong date - Date should be smaller than the current date"
							+ ANSI_RESET);
				}
				else
				{
					List<Object> list = new ArrayList<>(Arrays.asList("data", sensor, from, to, interval));
					this.out.writeObject(list);
					Object received_object = this.ois.readObject();
					data_series = (DataSeries) received_object;
					if (data_series == null) {
						data_series = new DataSeries(0, 1, 1, array, array2);
						System.out.println(ANSI_RED + "[ERROR] Wrong parameters or there " +
								           "are no values for the selected date" + ANSI_RESET);

					}
				}

				return data_series;

			} catch (SocketTimeoutException e) {
				System.out.println(ANSI_RED + "[ERROR] Socket timeout" + ANSI_RESET);
			}

			catch (IOException e) {
				System.out.println(ANSI_RED + "[ERROR] Can't reach the server - " +
						           "check your internet Connection" + ANSI_RESET);

			} catch (ClassNotFoundException e) {
				System.out.println(ANSI_RED + "[ERROR] Class could not be found" + ANSI_RESET);

			}

			return data_series;

		});
		completableFuture.get();
		return completableFuture;
	}

	/**
	 * Second assignment.
	 */
	public CompletableFuture<List<ClusterDescriptor>> getClustering(Sensor sensor, long from, long to, long intervalClusters, long intervalPoints, int numberOfClusters) {
		throw new UnsupportedOperationException("TODO");
	}

	/**
	 * Second assignment.
	 */
	public CompletableFuture<List<MatchedCurve>> getSimilarity(String metric, long from, long to, long minSize, long maxSize, int maxResultCount, double[] ref) {
		throw new UnsupportedOperationException("TODO");
	}

	@FunctionalInterface
	public interface ConnectionEventHandler {
		void apply();
	}
}
