package at.tugraz.oo2.server;


import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;


/**
 * This class will hold the implementation of your server and handle connecting and connected clients.
 */
public final class AnalysisServer {
	/**
	 * The maximum number of similarity search jobs a client can handle in parallel. Only
	 * relevant for the second assignment.
	 */
	public static final int MAX_JOBS_PER_CLIENT = 5;
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_RESET = "\u001B[0m";

	private final int serverPort;
	private final InfluxConnection influxConnection;

	public AnalysisServer(InfluxConnection influxConnection, int serverPort) {
		this.serverPort = serverPort;
		this.influxConnection = influxConnection;

	}

	public void run() {
		try {
			Cache new_cache = new Cache();
			ServerSocket server_socket = new ServerSocket(this.serverPort);
			while (true) {
				try {
					Socket socket = server_socket.accept();
					ObjectOutputStream out_stream = new ObjectOutputStream(socket.getOutputStream());
					ObjectInputStream in_stream = new ObjectInputStream((socket.getInputStream()));
					Thread server_thread = new RequestHandler(socket, out_stream, in_stream, influxConnection, new_cache);
					server_thread.start();

				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
				}
			}
		} catch (IOException e) {
			System.out.println(ANSI_RED + "[ERROR] Can't send the data" + ANSI_RESET);
		}



		// TODO Start here with a loop accepting new client connections.
	}
}
