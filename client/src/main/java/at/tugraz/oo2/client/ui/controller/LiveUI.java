package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.Util;
import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.GUIMain;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
	private static  ScheduledFuture<?> sched_future;

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


		GUIMain.getStage().setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent we) {
				if(ses != null)
					ses.shutdownNow();

			}
		});

	}

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


	private void onConnectionOpened()  {

		 this.tvData.getStylesheets().add("/liveui.css");
		 ProgressBar pb = new ProgressBar();
		 Label fetching_data = new Label("Fetching Live Data");
		 fetching_data.setLayoutX(510);
		 fetching_data.setLayoutY(350);
		 fetching_data.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font: 20px 'Arial'");
		 pb.setLayoutX(540);
		 pb.setLayoutY(380);
		 this.getChildren().add(pb);
		 this.getChildren().add(fetching_data);

	 ses = new ScheduledThreadPoolExecutor(1);

		this.new_runnable = new Runnable() {
			 @Override
			 public void run() {

					 synchronized (clientConnection) {
						 List<Sensor> sensors = null;
						 try {
							 sensors = clientConnection.querySensors().get();
						 } catch (InterruptedException e) {
							 System.out.println("Disconnected!");
						 } catch (ExecutionException e) {
							 e.printStackTrace();
						 }
						 ObservableList<LiveData> live_data = FXCollections.observableArrayList();
						 for (int i = 0; i < sensors.size(); i++) {
							 DataPoint new_data_point = null;
							 try {

								 new_data_point = clientConnection.queryValue(sensors.get(i)).get();
								 LiveData live = new LiveData(sensors.get(i).getLocation(), sensors.get(i).getMetric());
								 live.data = new_data_point.getValue();
								 live.timestamp = Util.TIME_FORMAT.format(new Date(new_data_point.getTime()));
								 live_data.add(live);
							 } catch (InterruptedException e) {
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
									 getChildren().remove(pb);
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


	}
}
