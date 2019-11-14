package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
public class ConnectionUI extends GridPane {

	private final ClientConnection clientConnection;

	@FXML
	public Button connectButton;
	@FXML
	public Button disconnectButton;
	@FXML
	public TextField serverTextField;
	@FXML
	public TextField portTextField;
	@FXML
	public Label status;


	public ConnectionUI(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;
		FXMLLoader loader = new FXMLLoader();
		loader.setControllerFactory(c -> this);
		loader.setRoot(this);
		try {
			loader.load(getClass().getResource("/connection.fxml").openStream());
		} catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}

		clientConnection.addConnectionClosedListener(this::onConnectionClosed);
		clientConnection.addConnectionOpenedListener(this::onConnectionOpened);
	}

	private void onConnectionOpened() {
		setConnected(true);
		Platform.runLater(() -> status.setText("Connected!"));

	}

	private void onConnectionClosed() {
		setConnected(false);
		Platform.runLater(() -> status.setText("Disconnected!"));
	}

	private void setConnected(boolean connected) {
		connectButton.setVisible(!connected);
		disconnectButton.setVisible(connected);
		serverTextField.setDisable(connected);
		portTextField.setDisable(connected);
	}


	public void onConnectClick(ActionEvent actionEvent) throws IOException {

		try {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						Thread.sleep(200);
						if(clientConnection.getConnectedButton())
						{
							clientConnection.setRunning(false);
							clientConnection.close();
						}

					} catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
						e.printStackTrace();
					}
				}
			});
			clientConnection.setConnectedButton(true);
			clientConnection.setRunning(true);
			if((serverTextField.getText() == null || serverTextField.getText().trim().isEmpty())
			   || !serverTextField.getText().equals("localhost"))
			{

				Alert alert = new Alert(Alert.AlertType.NONE);
				alert.setAlertType(Alert.AlertType.ERROR);
				alert.setContentText("Please enter the valid server name");
				alert.show();
				return;
			}
			else if(portTextField.getText() == null ||
					portTextField.getText().isEmpty()
					|| !portTextField.getText().equals("3888"))
			{
				Alert alert = new Alert(Alert.AlertType.NONE);
				alert.setAlertType(Alert.AlertType.ERROR);
				alert.setContentText("Please enter the valid port");
				alert.show();
				return;

			}
			String server_url = serverTextField.getText();
			int port = Integer.parseInt(portTextField.getText());
			if(!clientConnection.connect(server_url, port))
			{
				Alert alert = new Alert(Alert.AlertType.NONE);
				alert.setAlertType(Alert.AlertType.ERROR);
				alert.setContentText("Can't reach the server");
				alert.show();
				return;

			}
		}catch (NullPointerException e)
		{
			Alert alert = new Alert(Alert.AlertType.NONE);
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Please enter sever name and port");
			alert.show();
		}


	}

	public void onDisconnectClick(ActionEvent actionEvent) {

			clientConnection.setConnectedButton(false);
   			clientConnection.setRunning(false);
			clientConnection.close();


	}
}
