package at.tugraz.oo2.server;

/**
 * Used to start the server. The actual implementation should be in other classes.
 */
public final class ServerMain {

	public static void main(String... args) {
		if (args.length == 5) {
			final String influxUrl = args[0];
			final String influxDatabaseName = args[1];
			final String influxUser = args[2];
			final String influxPassword = args[3];
			final int serverPort = Integer.parseUnsignedInt(args[4]);
			final InfluxConnection influxConnection = new InfluxConnection(influxUrl, influxDatabaseName, influxUser, influxPassword);
			final AnalysisServer server = new AnalysisServer(influxConnection, serverPort);
			server.run();
			// control flow never reaches here
		} else {
			printUsage();
		}
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("  ./server.jar <influx url> <influx database name> <server username> <server password> <server port> - Starts the server");
	}
}
