package at.tugraz.oo2.client;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import at.tugraz.oo2.Util;
import at.tugraz.oo2.data.ClusterDescriptor;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.MatchedCurve;
import at.tugraz.oo2.data.Sensor;

/**
 * Used for handling and parsing commands. There is little to no work to do for you here, since
 * the CLI is already implemented.
 */
public final class CommandHandler {
	private static final String MSG_HELP = "Type 'help' for a list of commands.";

	private final ClientConnection conn;
	private final Map<String, Command> commands = new HashMap<>();

	public CommandHandler(ClientConnection conn) {
		this.conn = conn;
		commands.put("help", this::displayHelp);
		commands.put("ls", this::listSensors);
		commands.put("now", this::queryNow);
		commands.put("data", this::queryData);
		commands.put("cluster", this::getClustering);
		commands.put("sim", this::getSimilarity);
	}

	public void handle(String... args) {
		final Command cmd = commands.get(args[0].toLowerCase());
		if (cmd == null) {
			System.out.println("Unknown command. " + MSG_HELP);
			return;
		}
		try {
			cmd.handle(Arrays.copyOfRange(args, 1, args.length));
		} catch (final CommandException | NumberFormatException ex) {
			System.out.println("Error: " + ex.getMessage());
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	public void openCLI() {
		System.out.println("Welcome to the command line interface. " + MSG_HELP);
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				final String line;
				System.out.print("> ");
				try {
					line = scanner.nextLine().trim();
				} catch (final NoSuchElementException ex) { // EOF
					break;
				}
				if (line.startsWith("#")) {
					System.out.println(line);
				} else if (line.equalsIgnoreCase("exit")) {
					break;
				} else if (!line.isEmpty()) {
					handle(line.split("\\s+"));
				}
			}
		}
		System.out.println("Bye!");
	}

	@FunctionalInterface
	private interface Command {
		void handle(String... args) throws Exception;
	}

	private static void validateArgc(String[] args, int argc) throws CommandException {
		if (args.length != argc) {
			throw new CommandException("Invalid usage. " + MSG_HELP);
		}
	}

	private void queryNow(String... args) throws Exception {
		validateArgc(args, 2);
		final String location = args[0];
		final String metric = args[1];
		final DataPoint value = conn.queryValue(new Sensor(location, metric)).get();
		System.out.println("Current value:");
		printDataPoint(value);
	}

	private void listSensors(String... args) throws Exception {
		validateArgc(args, 0);
		final List<Sensor> sensors = conn.querySensors().get();
		System.out.println("Sensors:");
		sensors.stream().map(sensor -> '\t' + sensor.getLocation() + " - " + sensor.getMetric()).sorted(String::compareTo).forEach(System.out::println);
	}

	/**
	 * Is thrown when command arguments are wrong or a trivial error occurs,
	 * such as an invalid sensor.
	 *
	 * @author sanx
	 */
	private static final class CommandException extends Exception {
		public CommandException(String message) {
			super(message);
		}
	}

	private void queryData(String... args) throws Exception {
		validateArgc(args, 5);
		final String location = args[0];
		final String metric = args[1];
		final long from = Util.parseUserTime(args[2]);
		final long to = Util.parseUserTime(args[3]);
		final long interval = Integer.parseUnsignedInt(args[4]) * 60 * 1000; // interval given in min
		final DataSeries series = conn.queryData(new Sensor(location, metric), from, to, interval).get();
		System.out.println("Result with " + series.getPresentValueCount() + " values:");
		series.forEach(CommandHandler::printDataPoint);
	}

	private void getClustering(String... args) throws Exception {
		validateArgc(args, 7);
		final String location = args[0];
		final String metric = args[1];
		final long from = Util.parseUserTime(args[2]);
		final long to = Util.parseUserTime(args[3]);
		final long intervalClusters = Integer.parseUnsignedInt(args[4]) * 60 * 1000; // interval given in min
		final long intervalPoints = Integer.parseUnsignedInt(args[5]) * 60 * 1000; // interval given in min
		final int numberOfClusters = Integer.parseUnsignedInt(args[6]);
		final List<ClusterDescriptor> clusters = conn.getClustering(new Sensor(location, metric), from, to, intervalClusters, intervalPoints, numberOfClusters).get();
		System.out.println("Got " + clusters.size() + " with total error of " + ClusterDescriptor.getTotalError(clusters) + ":");
		for (int i = 0; i < clusters.size(); i++) {
			final ClusterDescriptor cluster = clusters.get(i);
			System.out.println("\tCluster #" + i + " with error of " + cluster.getClusterError() + ":");
			for (final DataSeries member : cluster.getMembers()) {
				System.out.println("\t\t" + Util.TIME_FORMAT.format(new Date(member.getMinTime())) + '\t' + cluster.getErrorOf(member));
			}
		}
	}

	private void getSimilarity(String... args) throws Exception {
		if (args.length < 7) {
			throw new CommandException("Invalid usage." + MSG_HELP);
		}
		final String metric = args[0];
		final long from = Util.parseUserTime(args[1]);
		final long to = Util.parseUserTime(args[2]);
		final long minSize = Integer.parseUnsignedInt(args[3]) * 60 * 1000; // interval given in min
		final long maxSize = Integer.parseUnsignedInt(args[4]) * 60 * 1000; // interval given in min
		final int maxResultCount = Integer.parseUnsignedInt(args[5]);
		final double[] ref = Arrays.stream(Arrays.copyOfRange(args, 6, args.length)).mapToDouble(Double::parseDouble).toArray();
		final List<MatchedCurve> matches = conn.getSimilarity(metric, from, to, minSize, maxSize, maxResultCount, ref).get();
		System.out.println("Got " + matches.size() + " matched curves:");
		for (int i = 0; i < matches.size(); i++) {
			final MatchedCurve match = matches.get(i);
			final DataSeries series = match.getSeries();
			System.out.println("\t" + match.getSensor().getLocation() + " " + Util.TIME_FORMAT.format(new Date(series.getMinTime())) + " length=" + (series.getLength() / 1000) + "s error=" + match.getError());
		}
	}

	private void displayHelp(String... args) {
		System.out.println("Usage:");
		System.out.println("  ls\t- Lists all sensors and metrics.");
		System.out.println("  now <location> <metric>\t- Returns the last measured value of a sensor.");
		System.out.println("  data <location> <metric> <from-time> <to-time> <interval-minutes>\t- Displays historic values measures by a sensor.");
		System.out.println("  cluster <location> <metric> <from-time> <to-time> <intervalClusters-minutes> <intervalPoints-minutes> <numClusters>\t- Clusters a measured time series.");
		System.out.println("  sim <metric> <from-time> <to-time> <minSize-minutes> <maxSize-minutes> <maxResultCount> <ref ...>\t- Performs sliding window similarity search using a reference curve.");
		System.out.println("  exit\t- Terminate the CLI.");
		System.out.println("More information is contained in the assignment description and in the folder queries/.");
		System.out.println();
	}

	private static void printDataPoint(DataPoint point) {
		System.out.println('\t' + Util.TIME_FORMAT.format(new Date(point.getTime())) + '\t' + point.getValue());
	}
}
