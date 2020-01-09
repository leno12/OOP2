package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.Util;
import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.GUIMain;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.Sensor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;
import lombok.Data;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class LiveUI extends AnchorPane {
	@Data
	public static class LiveData {
		private final String location;
		private final String metric;
		private Double data = null;
		private String timestamp = null;

	}
	private  ScheduledExecutorService executor;
	private final ClientConnection clientConnection;
	private static	ScheduledThreadPoolExecutor ses;
	private Runnable new_runnable;
	public static  ScheduledFuture<?> sched_future;
	List<Sensor> sensors = null;
	boolean init = true;
	long time_interval = System.currentTimeMillis();

	@FXML
	private TableView<LiveData> tvData;

	public LiveUI(ClientConnection clientConnection) {

		this.clientConnection = clientConnection;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/live.fxml"));
		loader.setRoot(this);
		loader.setController(this);

		try {
			loader.load();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		clientConnection.addConnectionOpenedListener(this::onConnectionOpened);
		clientConnection.addConnectionClosedListener(this::onConnectionClosed);



	}

	/**
	 * Cancel and shutdown scheduled executor service
 	 */
	private void onConnectionClosed()  {

			try {
					sched_future.cancel(true);
					ses.shutdown();
					ses.awaitTermination(10, TimeUnit.SECONDS);


			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		ses.shutdownNow();

	}

	/**
	 * Fetch all latest data for every sensor and location available
	 * Display values
	 * Refresh every 10 seconds
	 */
	private void onConnectionOpened()  {

		ProgressBar pb = new ProgressBar();
		Label fetching_data = new Label("Fetching Live Data");
		HBox new_progress_bar = new HBox();
		this.createProgressBar(fetching_data,pb,new_progress_bar);


	    ses = new ScheduledThreadPoolExecutor(1);

		this.new_runnable = new Runnable() {
			 @Override
			 public void run() {
					 synchronized (clientConnection) {


						 try {
						 	if(init || (System.currentTimeMillis() > time_interval + 20 * 60 * 1000))
						 	{
						 		init = false;
								sensors = clientConnection.querySensors().get();
								time_interval = System.currentTimeMillis();
							}
							 if(sensors == null)
							 {
								 Platform.runLater(() -> {
									 getChildren().remove(fetching_data);
									 getChildren().remove(new_progress_bar);
									 Alert alert = new Alert(Alert.AlertType.NONE);
									 alert.setAlertType(Alert.AlertType.ERROR);
									 alert.setContentText("Can't reach the server please try to disconnect " +
											 "and then connect again");
									 alert.show();
								 });
								 sched_future.cancel(true);
								 ses.shutdown();
								 ses.awaitTermination(10, TimeUnit.SECONDS);
								 return;
							 }
						 } catch (InterruptedException e)
						 {
							 Platform.runLater(new Runnable() {
								 @Override
								 public void run() {
									 getChildren().remove(fetching_data);
									 getChildren().remove(new_progress_bar);
								 }
							 });
							 System.out.println("Disconnected!");
						 } catch (ExecutionException e) {
							 e.printStackTrace();
						 }
						 ObservableList<LiveData> live_data = FXCollections.observableArrayList();

						 for (int i = 0; i < sensors.size(); i++) {
							 DataPoint new_data_point = null;
							 try {
								 if(!clientConnection.getRunning())
								 {
									 Platform.runLater(new Runnable() {
										 @Override
										 public void run() {
											 getChildren().remove(fetching_data);
											 getChildren().remove(new_progress_bar);
										 }
									 });
									 return;
								 }
								 new_data_point = clientConnection.queryValue(sensors.get(i)).get();
								 LiveData live = new LiveData(sensors.get(i).getLocation(), sensors.get(i).getMetric());
								 live.data = new_data_point.getValue();
								 live.timestamp = Util.TIME_FORMAT.format(new Date(new_data_point.getTime()));
								 live_data.add(live);
							 } catch (InterruptedException e)
							 {
								 Platform.runLater(new Runnable() {
								  @Override
									public void run() {
									  getChildren().remove(fetching_data);
									  getChildren().remove(new_progress_bar);
								  }
								  });

								 System.out.println("Disconnected!");
							 } catch (ExecutionException e) {
								 e.printStackTrace();
							 }

						 }
						 Platform.runLater(new Runnable() {
							 @Override
							 public void run() {
								 tvData.setItems(live_data);
								 if (!tvData.getItems().isEmpty()) {
									 getChildren().remove(new_progress_bar);
									 getChildren().remove(fetching_data);
									 //pb.setVisible(false);
								 }
							 }
						 });
					 }
			 }
		 };

		ses.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
		sched_future = ses.scheduleAtFixedRate(new_runnable , 0, 10, TimeUnit.SECONDS);


	}


	@FXML
	public void initialize() {
		TableColumn sensorColumn = new TableColumn<LiveData, Object>("Sensor");
		TableColumn<LiveData, String> locationColumn = new TableColumn<>("Location");
		TableColumn<LiveData, String> metricColumn = new TableColumn<>("Metric");
		sensorColumn.getColumns().addAll(locationColumn, metricColumn);
		TableColumn<LiveData, String> dataColumn = new TableColumn<>("Data");
		TableColumn<LiveData, String> timestampColumn = new TableColumn<>("Last update");


		tvData.getColumns().addAll(sensorColumn, dataColumn, timestampColumn);
		locationColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("location"));
		metricColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("metric"));
		dataColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("data"));
		timestampColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("timestamp"));
		tvData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.tvData.getStylesheets().add("/liveui.css");


	}

	/**
	 * Create progress bar so the user knows that data is being collected from the server
	 * @param fetching_data
	 * @param pb
	 * @param new_progress_bar
	 */
	public void createProgressBar(Label fetching_data, ProgressBar pb, HBox new_progress_bar)
	{
		fetching_data.setMaxWidth(Double.MAX_VALUE);
		AnchorPane.setLeftAnchor(fetching_data, 0.0);
		AnchorPane.setRightAnchor(fetching_data, 0.0);
		AnchorPane.setBottomAnchor(fetching_data, 0.0);
		AnchorPane.setTopAnchor(fetching_data, -80.0);
		fetching_data.setAlignment(Pos.CENTER);
		AnchorPane.setLeftAnchor(new_progress_bar, 0.0);
		AnchorPane.setRightAnchor(new_progress_bar, 0.0);
		AnchorPane.setBottomAnchor(new_progress_bar, 0.0);
		AnchorPane.setTopAnchor(new_progress_bar, 0.0);
		new_progress_bar.setAlignment(Pos.CENTER);
		new_progress_bar.getChildren().add(pb);
		this.getChildren().add(new_progress_bar);
		fetching_data.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font: 20px 'Arial'");
		this.getChildren().add(fetching_data);
	}



}
