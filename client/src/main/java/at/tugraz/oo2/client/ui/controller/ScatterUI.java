package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.component.DateTimePicker;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.RangeSlider;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScatterUI extends HBox {

	private final ClientConnection clientConnection;
	private Label fetching_data_status;
	private Label fetching_data_status_y;

	@FXML
	private ListView<String> lvSensorX;
	@FXML
	private ListView<String> lvSensorY;

	@FXML
	private DateTimePicker dpFrom;
	@FXML
	private DateTimePicker dpTo;
	@FXML
	private Spinner<Integer> spInterval;
	@FXML
	public Button drawChartButton;
	@FXML
	private Label lbFromToSlider;
	@FXML
	private AnchorPane ap;

	@FXML
	private RangeSlider slDay;



	public ScatterUI(ClientConnection clientConnection) {
		this.fetching_data_status = null;
		this.fetching_data_status_y = null;
		this.clientConnection = clientConnection;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/scatter.fxml"));
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

		Label progress_label = new Label("Fetching sensor data");
		progress_label.setLayoutX(280);
		progress_label.setLayoutY(220);
		progress_label.setStyle("-fx-text-fill: white; -fx-font-weight: bold");
		ProgressBar pb = new ProgressBar();
		pb.setLayoutX(300);
		pb.setLayoutY(250);
		ap.getChildren().add(pb);
		ap.getChildren().add(progress_label);
		Runnable task = new Runnable()
		{
			public void run()
			{
				synchronized (clientConnection) {

					try {
						if(!clientConnection.getRunning())
							return;

						List<Sensor> sensors = clientConnection.querySensors().get();
						ObservableList<String> live_data = FXCollections.observableArrayList();

						for (int i = 0; i < sensors.size(); i++) {
							String new_sensor = sensors.get(i).getLocation() + " - " + sensors.get(i).getMetric();
							live_data.add(new_sensor);

						}
						Platform.runLater(new Runnable()
						{
							@Override
							public void run()
							{
								lvSensorX.setItems(live_data);
								lvSensorY.setItems(live_data);
								ap.getChildren().remove(progress_label);
								ap.getChildren().remove(pb);
							}
						});

					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

				}

			}
		};

		// Run the task in a background thread
		Thread backgroundThread = new Thread(task);
		// Terminate the running thread if the application exits
		backgroundThread.setDaemon(true);
		// Start the thread
		backgroundThread.start();


	}

	@FXML
	public void initialize() {

		this.getStylesheets().add("/scatterui.css");
		spInterval.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 10000, 60, 5));



	}
	@FXML
	public void drawScatterPlotButton() {
		this.getChildren().remove(this.lookup("#chart"));

		Alert alert = new Alert(Alert.AlertType.NONE);
		Alert alert2 = new Alert(Alert.AlertType.NONE);

		if(lvSensorX.getSelectionModel().isEmpty() || lvSensorY.getSelectionModel().isEmpty())
		{

			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Please choose two sensors");
			alert.show();
			return;
		}
		else if(dpFrom.getValue() == null || dpTo.getValue() == null)
		{
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Please select date");
			alert.show();
			return;
		}
		Label progress_label = new Label("Creating graph");
		progress_label.setMaxWidth(Double.MAX_VALUE);
		AnchorPane.setLeftAnchor(progress_label, 0.0);
		AnchorPane.setRightAnchor(progress_label, 0.0);
		AnchorPane.setBottomAnchor(progress_label, 0.0);
		AnchorPane.setTopAnchor(progress_label, -80.0);
		progress_label.setAlignment(Pos.CENTER);
		progress_label.setStyle("-fx-text-fill: white; -fx-font-weight:bold; -fx-font: 20px 'Arial'");
		ProgressBar pb = new ProgressBar();
		HBox new_progress_bar = new HBox();
		AnchorPane.setLeftAnchor(new_progress_bar, 0.0);
		AnchorPane.setRightAnchor(new_progress_bar, 0.0);
		AnchorPane.setBottomAnchor(new_progress_bar, 0.0);
		AnchorPane.setTopAnchor(new_progress_bar, 0.0);
		new_progress_bar.setAlignment(Pos.CENTER);
		new_progress_bar.getChildren().add(pb);
		ap.getChildren().add(new_progress_bar);
		ap.getChildren().add(progress_label);
		new Thread(() -> {

			synchronized (clientConnection) {
				try {

					String selected_sensor_x = lvSensorX.getSelectionModel().getSelectedItem();
					String selected_sensor_y = lvSensorY.getSelectionModel().getSelectedItem();

					String current_sensor[] = selected_sensor_x.split("-");
					String location = current_sensor[0].replaceAll("\\s+", "");
					String metric = current_sensor[1].replaceAll("\\s+", "");
					Sensor sensor_x = new Sensor(location, metric);
					String current_sensor_y[] = selected_sensor_y.split("-");
					location = current_sensor_y[0].replaceAll("\\s+", "");
					metric = current_sensor_y[1].replaceAll("\\s+", "");
					Sensor sensor_y = new Sensor(location, metric);

					LocalDateTime localDate = dpFrom.getValue();
					Instant instant = localDate.atZone(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();
					Date date = Date.from(instant);
					long date_from = date.getTime();
					localDate = dpTo.getValue();
					instant = localDate.atZone(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();
					date = Date.from(instant);
					long date_to = date.getTime();
					long interval = spInterval.getValue() * 60 * 1000;
					if(date_from >= date_to)
					{
						Platform.runLater(() -> {
							alert.setAlertType(Alert.AlertType.ERROR);
							alert.setContentText("End Date should be bigger than Start Date");
							alert.show();
							return;
						});
					}
					this.drawScatterPlot(sensor_x,sensor_y ,date_from, date_to, interval);
					Platform.runLater(() -> {
						ap.getChildren().remove(progress_label);
						ap.getChildren().remove(new_progress_bar);
					});



				} catch (NullPointerException e) {
					Platform.runLater(() -> {
						alert2.setAlertType(Alert.AlertType.ERROR);
						alert2.setContentText("Please check selected items");
						alert2.show();
					});

				}
			}

		}).start();


	}
	public void drawScatterPlot(Sensor sensor_x,Sensor sensor_y, long date_from, long date_to, long interval)
	{

		try {

			DataSeries new_data_series_x = clientConnection.queryData(sensor_x, date_from, date_to, interval).get();
			List<DataPoint> data_points_x = new_data_series_x.getDataPoints();
			DataSeries new_data_series_y = clientConnection.queryData(sensor_y, date_from, date_to, interval).get();
			List<DataPoint> data_points_y = new_data_series_y.getDataPoints();

			final NumberAxis xAxis = new NumberAxis();
			final NumberAxis yAxis = new NumberAxis();
			String x_label = sensor_x.getLocation() + "/" + sensor_x.getMetric();
			xAxis.setLabel(x_label);
			String y_label = sensor_y.getLocation() + "/" + sensor_y.getMetric();
			yAxis.setLabel(y_label);

			final ScatterChart<Number, Number> scatterChart =
					new ScatterChart<Number, Number>(xAxis, yAxis);
			scatterChart.setTitle("Scatter Plot");
			XYChart.Series series1 = new XYChart.Series();
			series1.setName("Scatter Chart");
			scatterChart.setHorizontalGridLinesVisible(false);
			scatterChart.setVerticalGridLinesVisible(false);
			scatterChart.getStylesheets().add("/chart.css");
			for (int i = 0; i < data_points_x.size(); i++) {
				Double value_x = data_points_x.get(i).getValue();
				Double value_y = data_points_y.get(i).getValue();

				series1.getData().add(new XYChart.Data(value_x, value_y));
			}
			scatterChart.getData().add(series1);

			if(!clientConnection.getMaximised())
			{
				scatterChart.setPrefWidth(800);
			}
			else
			{
				scatterChart.setPrefWidth(1500);
			}
			scatterChart.setPrefHeight(900);
			AnchorPane.setLeftAnchor(scatterChart, 0.0);
			AnchorPane.setRightAnchor(scatterChart, 0.0);
			AnchorPane.setBottomAnchor(scatterChart, 0.0);
			AnchorPane.setTopAnchor(scatterChart, 0.0);


			Platform.runLater(() -> {
				this.ap.getChildren().addAll(scatterChart);
			});

		}catch (ExecutionException | InterruptedException e )
		{
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.NONE);
				alert.setAlertType(Alert.AlertType.ERROR);
				alert.setContentText("Please check selected items");
				alert.show();
			});
		}
	}
}
