package at.tugraz.oo2.client;

import at.tugraz.oo2.client.ui.GUIMain;

import java.io.IOException;
import java.util.Arrays;

public final class ClientMain {

	public static void main(String... args) {
		if (args.length == 0) {
			GUIMain.openGUI(args);
		} else if (args.length >= 2) {
			final String url = args[0];
			final int port = Integer.parseUnsignedInt(args[1]);
			try {
				ClientConnection conn = new ClientConnection();
				conn.addConnectionClosedListener(() -> System.out.println("Client disconnected."));
				if(!conn.connect(url, port))
					return;
				final CommandHandler handler = new CommandHandler(conn);
				if (args.length == 2) {
					handler.openCLI();
				} else {
					handler.handle(Arrays.copyOfRange(args, 2, args.length));
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		} else {
			printUsage();
		}
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("  ./client.jar - Opens the GUI");
		System.out.println("  ./client.jar <server url> <server port> - Connects to the server and opens the CLI");
		System.out.println("  ./client.jar <server url> <server port> <cmd> <params ...> - Connects to the server and executes one command");
	}
}
