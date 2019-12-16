package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.component.DurationPicker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class SketchUI extends HBox {

	private final ClientConnection clientConnection;

	@FXML
	private ListView<String> lvMetric;
	@FXML
	private DatePicker dpFrom;
	@FXML
	private DatePicker dpTo;
	@FXML
	private DurationPicker dpMinSize;
	@FXML
	private DurationPicker dpMaxSize;
	@FXML
	private Spinner<Integer> spMaxResultCount;

	public SketchUI(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/sketch.fxml"));
		loader.setRoot(this);
		loader.setController(this);

		try {
			loader.load();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		clientConnection.addConnectionOpenedListener(this::onConnectionOpened);
	}

	private void onConnectionOpened() {

	}

	@FXML
	public void initialize() {
		dpMinSize.setDuration(60*60);
		dpMaxSize.setDuration(60*60*24*2);
		spMaxResultCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 16, 5));
	}

}
