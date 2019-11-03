package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;


public class AssignmentUI extends TabPane {

	public AssignmentUI(ClientConnection clientConnection) {
		this.setDisable(true);
		Tab liveTab = new Tab("Live data");
		Tab lineChartTab = new Tab("Line chart");
		Tab scatterTab = new Tab("Scatter");
		getTabs().add(liveTab);
		getTabs().add(lineChartTab);
		getTabs().add(scatterTab);

		liveTab.setContent(new LiveUI(clientConnection));
		lineChartTab.setContent(new LineChartUI(clientConnection));
		scatterTab.setContent(new ScatterUI(clientConnection));

		this.getSelectionModel().select(liveTab);

		setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		clientConnection.addConnectionOpenedListener(this::onConnectionOpened);
		clientConnection.addConnectionClosedListener(this::onConnectionClosed);
	}

	private void onConnectionOpened() {
		this.setDisable(false);
	}

	private void onConnectionClosed() {
		this.setDisable(true);
	}

}
