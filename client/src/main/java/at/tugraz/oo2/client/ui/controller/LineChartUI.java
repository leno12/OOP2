package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.GUIMain;
import at.tugraz.oo2.client.ui.component.DateTimePicker;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
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
	private DateTimePicker dpFrom;
	@FXML
	private DateTimePicker dpTo;
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

	/**
	 * Get avaliable sensors and display them in list view
	 */

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


		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();




	}

	/**
	 * Get data needed to draw a linear chart when draw Chart Button is pressed
	 * Create new thread so that GUI stays responsive
	 * @param event
	 */
	@FXML protected void drawChartButton(ActionEvent event) {


		this.getChildren().remove(this.lookup("#chart"));
		Alert alert = new Alert(Alert.AlertType.NONE);
		Alert alert2 = new Alert(Alert.AlertType.NONE);
		if(!LineChartUI.checkParameters(lvSensors,null,dpFrom,dpTo))
			return;

		Label progress_label = new Label("Creating graph");
		ProgressBar pb = new ProgressBar();
		HBox new_progress_bar = new HBox();
		this.createProgressBar(pb,progress_label,new_progress_bar);

		new Thread(() -> {

					synchronized (clientConnection) {
						try {

							String selected_sensor = lvSensors.getSelectionModel().getSelectedItem();

							String current_sensor[] = selected_sensor.split("-");
							String location = current_sensor[0].replaceAll("\\s+", "");
							String metric = current_sensor[1].replaceAll("\\s+", "");
							Sensor sensor = new Sensor(location, metric);

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
							drawLineChart(sensor, date_from, date_to, interval, selected_sensor);
							Platform.runLater(() -> {
								this.getChildren().remove(progress_label);
								this.getChildren().remove(new_progress_bar);

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

	/**
	 * Draws a line chart and displays it
	 * @param sensor
	 * @param date_from
	 * @param date_to
	 * @param interval
	 * @param selected_sensor
	 */
	public void drawLineChart(Sensor sensor, long date_from, long date_to, long interval, String selected_sensor)
	{

		try {

			DataSeries new_data_series = clientConnection.queryData(sensor, date_from, date_to, interval).get();
			List<DataPoint> data_points = new_data_series.getDataPoints();
			final NumberAxis xAxis = new NumberAxis();
			/*xAxis.setTickLabelFormatter(new StringConverter<Number>() {
				@Override
				public String toString(Number number) {
					double test = number.doubleValue() * 3600000.0;
					Date new_date = new Date((long)test);
					return new_date.toString();
				}

				@Override
				public Number fromString(String s) {
					System.out.println("problem\n");
					return null;
				}
			});*/
			//xAxis.setAutoRanging(false);
			xAxis.setTickLabelRotation(90);
			final NumberAxis yAxis = new NumberAxis();
			//yAxis.translateXProperty().bind(xAxis.widthProperty().divide(2));
			xAxis.setLabel("Interval");
			final LineChart<Number, Number> lineChart =
					new LineChart<Number, Number>(xAxis, yAxis);
			lineChart.setCreateSymbols(false);
			lineChart.setTitle(selected_sensor);
			XYChart.Series series1 = new XYChart.Series();
			series1.setName("Line Chart");
			lineChart.setHorizontalGridLinesVisible(false);
			lineChart.setVerticalGridLinesVisible(false);
			lineChart.getStylesheets().add("/chart.css");
			long inc = 0;
			for (int i = 0; i < data_points.size(); i++) {
				Double value = data_points.get(i).getValue();
				series1.getData().add(new XYChart.Data((double)inc/60000.0, value));
				inc += interval;
			}
			lineChart.getData().add(series1);
			VBox chart = new VBox();
			chart.setId("chart");
			chart.setStyle("-fx-background-color: #000000;");
			AnchorPane.setLeftAnchor(chart, 250.0);
			AnchorPane.setRightAnchor(chart, 0.0);
			AnchorPane.setBottomAnchor(chart, 0.0);
			AnchorPane.setTopAnchor(chart, 0.0);
			chart.setAlignment(Pos.CENTER);

			if(!clientConnection.getMaximised())
			{
				lineChart.setPrefWidth(800);
			}
			else
			{
				lineChart.setPrefWidth(1500);
			}
			lineChart.setPrefHeight(900);


			chart.getChildren().addAll(lineChart);

			Platform.runLater(() -> {
				this.getChildren().add(chart);
			});

			GUIMain.getStage().maximizedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {

					if(t1.booleanValue())
					{
						lineChart.setPrefWidth(1500);
						lineChart.setPrefHeight(900);
						clientConnection.setMaximised(true);
					}
					else
					{
						lineChart.setPrefWidth(900);
						lineChart.setPrefHeight(900);
						clientConnection.setMaximised(false);

					}
				}
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

	/**
	 * Created fetching data Animation so that user knows that sensor data is being fetched
	 * @param fetching_data_status
	 */

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

	/**
	 * Checks if user input is correct (Sensor, Date, Interval)
	 * @param lvSensors
	 * @param lvSensorsY
	 * @param dpFrom
	 * @param dpTo
	 * @return
	 */
	public static boolean checkParameters(ListView<String> lvSensors, ListView<String> lvSensorsY, DateTimePicker dpFrom, DateTimePicker dpTo)
	{
		Alert alert = new Alert(Alert.AlertType.NONE);
		Alert alert2 = new Alert(Alert.AlertType.NONE);
		if(lvSensors.getSelectionModel().isEmpty() || (lvSensorsY != null && lvSensorsY.getSelectionModel().isEmpty()))
		{

			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Please choose one sensor");
			alert.show();
			return false;
		}
		else if(dpFrom.getValue() == null || dpTo.getValue() == null)
		{
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Please select date");
			alert.show();
			return false;
		}

		else if(dpFrom.getValue().isAfter(LocalDateTime.now()) || dpTo.getValue().isAfter(LocalDateTime.now()))
		{
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Date should be smaller than the current date");
			alert.show();
			return false;
		}
		else if(dpFrom.getValue().isAfter(dpTo.getValue()))
		{
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("End Date should be bigger than Start Date");
			alert.show();
			return false;
		}
     return true;
	}

	/**
	 * Create progress bar so that user knows that graph is being created or sensor data is being fetched
	 * @param pb
	 * @param progress_label
	 * @param new_progress_bar
	 */
	public void createProgressBar(ProgressBar pb, Label progress_label, HBox new_progress_bar)
	{
		progress_label.setMaxWidth(Double.MAX_VALUE);
		AnchorPane.setLeftAnchor(progress_label, 300.0);
		AnchorPane.setRightAnchor(progress_label, 0.0);
		AnchorPane.setBottomAnchor(progress_label, 0.0);
		AnchorPane.setTopAnchor(progress_label, -80.0);
		progress_label.setAlignment(Pos.CENTER);
		progress_label.setStyle("-fx-text-fill: white; -fx-font-weight:bold; -fx-font: 20px 'Arial'");
		AnchorPane.setLeftAnchor(new_progress_bar, 300.0);
		AnchorPane.setRightAnchor(new_progress_bar, 0.0);
		AnchorPane.setBottomAnchor(new_progress_bar, 0.0);
		AnchorPane.setTopAnchor(new_progress_bar, 0.0);
		new_progress_bar.setAlignment(Pos.CENTER);
		new_progress_bar.getChildren().add(pb);
		this.getChildren().add(new_progress_bar);
		this.getChildren().add(progress_label);
	}
}
