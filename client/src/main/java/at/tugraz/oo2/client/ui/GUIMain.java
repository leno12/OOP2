package at.tugraz.oo2.client.ui;

import at.tugraz.oo2.client.ui.controller.MainUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public final class GUIMain extends Application {
	private static Stage guiStage;

	public static Stage getStage() {
		return guiStage;
	}
	public static void openGUI(String... args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws IOException {
		stage.setTitle("OO2 Client");
		guiStage = stage;

		final Parent root = new MainUI();
		final Scene scene = new Scene(root,1200,1000);
		stage.setScene(scene);

		stage.show();
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent we) {
				Platform.exit();
				System.exit(0);
			}
		});

	}
}
