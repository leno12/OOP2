package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import com.github.javafx.charts.zooming.ZoomManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LineChartUI extends AnchorPane {
	private final ClientConnection clientConnection;
	private Label fetching_data_status;


	@FXML
	private ListView<String> lvSensors;
	@FXML
	private DatePicker dpFrom;
	@FXML
	private DatePicker dpTo;
	@FXML
	private Spinner<Integer> spInterval;
	@FXML
	public Button drawChartButton;




	public LineChartUI(ClientConnection clientConnection) {
		this.fetching_data_status = null;
		this.clientConnection = clientConnection;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/linechart.fxml"));
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
		if(fetching_data_status == null)
		{

			this.fetching_data_status = new Label("Fetching Data");
			this.fetchingDataAnimation(fetching_data_status);
		}
		Runnable task = new Runnable()
		{
			public void run()
			{
				synchronized (clientConnection) {

					try {

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
								lvSensors.setItems(live_data);
								getChildren().remove(fetching_data_status);
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
	@FXML protected void drawChartButton(ActionEvent event) {


		this.getChildren().remove(this.lookup("#chart"));
		Alert alert = new Alert(Alert.AlertType.NONE);
		Alert alert2 = new Alert(Alert.AlertType.NONE);

		if(lvSensors.getSelectionModel().isEmpty())
		{

				alert.setAlertType(Alert.AlertType.ERROR);
				alert.setContentText("Please choose one sensor");
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
		progress_label.setLayoutX(550);
		progress_label.setLayoutY(320);
		progress_label.setStyle("-fx-text-fill: white");
		ProgressBar pb = new ProgressBar();
		pb.setLayoutX(550);
		pb.setLayoutY(350);
		this.getChildren().add(pb);
		this.getChildren().add(progress_label);
		new Thread(() -> {

					synchronized (clientConnection) {
						try {

							String selected_sensor = lvSensors.getSelectionModel().getSelectedItem();

							String current_sensor[] = selected_sensor.split("-");
							String location = current_sensor[0].replaceAll("\\s+", "");
							String metric = current_sensor[1].replaceAll("\\s+", "");
							Sensor sensor = new Sensor(location, metric);

							LocalDate localDate = dpFrom.getValue();
							Instant instant = Instant.from(localDate.atStartOfDay(ZoneId.systemDefault()));
							Date date = Date.from(instant);
							long date_from = date.getTime();
							localDate = dpTo.getValue();
							instant = Instant.from(localDate.atStartOfDay(ZoneId.systemDefault()));
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
							drawLineChart(sensor, date_from, date_to, interval, selected_sensor);
							Platform.runLater(() -> {
								this.getChildren().remove(progress_label);
								this.getChildren().remove(pb);

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


	@FXML
	public void initialize() {
		lvSensors.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		spInterval.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 10000, 60, 5));
		this.setStyle("-fx-background-color: #000000;");
	}

	public void drawLineChart(Sensor sensor, long date_from, long date_to, long interval, String selected_sensor)
	{

		try {

			DataSeries new_data_series = clientConnection.queryData(sensor, date_from, date_to, interval).get();
			List<DataPoint> data_points = new_data_series.getDataPoints();

			final CategoryAxis xAxis = new CategoryAxis();
			final NumberAxis yAxis = new NumberAxis();
			xAxis.setLabel("Date");
			final LineChart<String, Number> lineChart =
					new LineChart<String, Number>(xAxis, yAxis);
			lineChart.setCreateSymbols(false);
			lineChart.setTitle(selected_sensor);
			//List<DataPoint> za_grafa =  lista_za_graf;
			XYChart.Series series1 = new XYChart.Series();
			series1.setName("Line Chart");
			lineChart.setHorizontalGridLinesVisible(false);
			lineChart.setVerticalGridLinesVisible(false);
			lineChart.getStylesheets().add("/chart.css");
			for (int i = 0; i < data_points.size(); i++) {
				Date date2 = new Date(data_points.get(i).getTime());
				String print_date = date2.toString().replace("CEST 2019", " ");
				Double value = data_points.get(i).getValue();

				series1.getData().add(new XYChart.Data(print_date, value));
			}
			lineChart.getData().add(series1);
			lineChart.setPrefWidth(650);
			HBox chart = new HBox();
			chart.setId("chart");
			chart.setStyle("-fx-background-color: #000000;");
		//	chart.prefWidthProperty().bind(this.widthProperty());

			chart.setLayoutX(250);
			chart.setLayoutY(70);
			//chart.autosize();
			lineChart.setPrefHeight(650);
			lineChart.autosize();


			chart.getChildren().addAll(lineChart);

			Platform.runLater(() -> {
				this.getChildren().add(chart);
			//	new ZoomManager(chart, lineChart, series1);
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

	private void fetchingDataAnimation(Label fetching_data_status)
	{

		final Timeline fetching_data_animation = new Timeline(
				new KeyFrame(Duration.ZERO, new EventHandler() {
					@Override public void handle(Event event) {
						String fetching_status = fetching_data_status.getText();
						if("Fetching Data . . .".equals(fetching_status))
							fetching_status =  "Fetching Data .";
						else
							fetching_status = fetching_status + " .";

						fetching_data_status.setText(fetching_status);
					}
				}),
				new KeyFrame(Duration.millis(1000))
		);
		fetching_data_animation.setCycleCount(Timeline.INDEFINITE);
		fetching_data_status.setStyle("-fx-text-fill: white; -fx-font-weight: bold");
		fetching_data_status.setLayoutX(70);
		fetching_data_status.setLayoutY(180);
		this.getChildren().add(fetching_data_status);
		fetching_data_animation.play();
	}
}
