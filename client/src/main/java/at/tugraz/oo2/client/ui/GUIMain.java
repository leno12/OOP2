package at.tugraz.oo2.client.ui;

import at.tugraz.oo2.client.ui.controller.MainUI;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
		final Scene scene = new Scene(root,900,800);
		stage.setScene(scene);
		stage.maximizedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
				System.out.println("maximized:" + t1.booleanValue());
			}
		});

		stage.show();
	}
}
