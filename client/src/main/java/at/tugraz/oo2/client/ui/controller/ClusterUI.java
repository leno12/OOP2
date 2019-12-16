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

public class ClusterUI extends HBox {

	private final ClientConnection clientConnection;

	@FXML
	private ListView<String> lvSensors;
	@FXML
	private DatePicker dpFrom;
	@FXML
	private DatePicker dpTo;
	@FXML
	private DurationPicker dpIntervalClusters;
	@FXML
	private Spinner<Integer> spPointsCluster;
	@FXML
	private Spinner<Integer> spClusters;

	public ClusterUI(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/cluster.fxml"));
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
		dpIntervalClusters.setDuration(24*60*60);
		spPointsCluster.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 24, 1));
		spClusters.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 2, 1));
	}

}
