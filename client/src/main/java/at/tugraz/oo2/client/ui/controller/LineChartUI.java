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
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
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
	@FXML
	public Button queryHistory;
	public String[] content_lines;


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
						if(sensors == null)
						{
							Platform.runLater(() -> {
								Alert alert = new Alert(Alert.AlertType.NONE);
								alert.setAlertType(Alert.AlertType.ERROR);
								alert.setContentText("Can't reach the server please try to disconnect " +
										"and then connect again");
								alert.show();
							});
							return;
						}
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
								LiveDataLineChartUI.executeTask();
								getChildren().remove(fetching_data_status);
							}
						});

					} catch (InterruptedException e)
					{
						e.printStackTrace();
					} catch (ExecutionException e)
					{
						e.printStackTrace();
					}

				}


			}
		};


		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
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

							ObservableList<String> selectedItems = lvSensors.getSelectionModel().getSelectedItems();

							String selected_sensor = selectedItems.get(0);

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
							drawLineChart(sensor, date_from, date_to, interval, selectedItems);
							Platform.runLater(() -> {
								this.getChildren().remove(progress_label);
								this.getChildren().remove(new_progress_bar);

							});

						} catch (NullPointerException e) {
							Platform.runLater(() -> {
								this.getChildren().remove(progress_label);
								this.getChildren().remove(new_progress_bar);
								alert2.setAlertType(Alert.AlertType.ERROR);
								alert2.setContentText("Disconnected");
								alert2.show();
							});

						}
					}


			}).start();

	}

	/**
	 * Get data needed to show Line Chart query history when Show recent queries Button is pressed
	 * @param event
	 */
	@FXML protected void queryHistory(ActionEvent event) {


		String hist = "LineChartHistory.txt";
		String content = null;
		try {
			content = Files.readString(Paths.get(hist), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.NONE);
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("No recent queries found");
			alert.show();
			return;

		}
		content_lines = content.split(System.getProperty("line.separator"));

		ListView<String> history = new ListView<String>();
		ObservableList<String> obs = FXCollections.observableArrayList(content_lines);
		history.setItems(obs);

		final Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(GUIMain.getStage());
		VBox dialogVbox = new VBox(20);
		dialogVbox.setVgrow(history, Priority.ALWAYS);
		history.setStyle("-fx-text-fill: white");
		dialogVbox.getChildren().add(history);
		Scene dialogScene = new Scene(dialogVbox, 400, 500);
		dialogVbox.setStyle("-fx-background-color: rgba(38, 38, 38, 0.85); -fx-text-fill: white");
		dialog.setScene(dialogScene);
		dialog.show();

		history.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				String selected_sensor = history.getSelectionModel().getSelectedItem();
				String[] arr = selected_sensor.split(" ");
				if(mouseEvent.getClickCount() == 2)
				{
					Date date_from = new Date(Long.parseLong(arr[2]));
					Date date_to = new Date(Long.parseLong(arr[3]));
					Integer inter = Integer.parseInt(arr[4]);
					inter /= 60000;

					dpFrom.setDate(date_from.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
					dpTo.setDate(date_to.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
					spInterval.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 10000, inter, 5));
					lvSensors.getSelectionModel().select(arr[0] + " - " + arr[1]);
					dialog.close();
				}
			}
		});
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
	public void drawLineChart(Sensor sensor, long date_from, long date_to, long interval, ObservableList<String> selected_sensor)
	{
		try {
			DataSeries new_data_series = clientConnection.queryData(sensor, date_from, date_to, interval).get();
			if(new_data_series.getMinTime() == -1)
			{
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.NONE);
					alert.setAlertType(Alert.AlertType.ERROR);
					alert.setContentText("No data could be found for the selected dates for sensor "
							+ sensor.getLocation() + "/" + sensor.getMetric());
					alert.show();
				});
				return;
			}
			else if(new_data_series.getMinTime() == 0)
			{
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.NONE);
					alert.setAlertType(Alert.AlertType.ERROR);
					alert.setContentText("Can't reach the server");
					alert.show();
				});
				return;
			}
			String history = "LineChartHistory.txt";
			FileWriter fw = null;
			try {
				fw = new FileWriter(history, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			BufferedWriter bw = new BufferedWriter(fw);
			String content = " ";
			Boolean todo = true;
			try {
				content = Files.readString(Paths.get(history), StandardCharsets.UTF_8);
				content_lines = content.split(System.getProperty("line.separator"));
				for(int i = 0; i < content_lines.length; ++i)
				{
					String line_check = sensor.getLocation() + " " + sensor.getMetric() + " " +
							date_from + " " + date_to + " " + interval;
					if(line_check.equals(content_lines[i]))
						todo = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(todo)
			{
				try {
					bw.write(sensor.getLocation() + " ");
					bw.write(sensor.getMetric() + " ");
					bw.write(date_from + " ");
					bw.write(date_to + " ");
					bw.write(interval + "\n");
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}


			List<DataPoint> data_points = new_data_series.getDataPoints();
			final NumberAxis xAxis = new NumberAxis();
			xAxis.setTickLabelRotation(90);
			final NumberAxis yAxis = new NumberAxis();
			xAxis.setLabel("Interval in minutes");
			yAxis.setLabel(sensor.getMetric());
			final LineChart<Number, Number> lineChart =
					new LineChart<Number, Number>(xAxis, yAxis);
			lineChart.setCreateSymbols(true);

			XYChart.Series series1 = new XYChart.Series();
			series1.setName("Line Chart");
			lineChart.setHorizontalGridLinesVisible(false);
			lineChart.setVerticalGridLinesVisible(false);
			lineChart.getStylesheets().add("/chart.css");
			lineChart.setLegendVisible(false);

			long inc = 0;
			for (int i = 0; i < data_points.size(); i++) {
				Double value = data_points.get(i).getValue();
				series1.getData().add(new XYChart.Data((double)inc/60000.0, value));
				inc += interval;
			}
			lineChart.getData().add(series1);
			for (XYChart.Series<Number, Number> s : lineChart.getData()) {
				for(int i = 0; i < s.getData().size(); i++)
				{

					DecimalFormat df = new DecimalFormat("#.##");
					Date date = new Date(data_points.get(i).getTime());
					String y_label = sensor.getLocation() + "/" + sensor.getMetric();
					String str = date.toString() + '\n' + y_label + " - " + df.format(s.getData().get(i).getYValue());
					Tooltip hover = new Tooltip(str);
					hover.setShowDelay(Duration.seconds(0));
					Tooltip.install(s.getData().get(i).getNode(), hover);
				}
			}




			lineChart.setPrefHeight(900);

            XYChart.Series new_series = null;
			final NumberAxis xAxis_2 = new NumberAxis();
			xAxis_2.setTickLabelRotation(90);
			final NumberAxis yAxis_2 = new NumberAxis();
			xAxis_2.setLabel("Interval in minutes");

			final LineChart<Number, Number> second_line_chart =
					new LineChart<Number, Number>(xAxis_2, yAxis_2)
					{
						{// hide xAxis in constructor, since not public
							getChartChildren().remove(getXAxis());
							// not getPlotChildren()
						}
					};

			second_line_chart.setHorizontalGridLinesVisible(false);
			second_line_chart.setVerticalGridLinesVisible(false);
			second_line_chart.getStylesheets().add("/test.css");
			yAxis_2.translateXProperty().bind(
					xAxis.widthProperty().add(25)
			);

			yAxis_2.setStyle("-fx-rotate: 360");
			yAxis_2.setTranslateZ(-1);
			yAxis_2.translateYProperty().bind(yAxis.maxHeightProperty());

			xAxis_2.setStyle("-fx-background-color:  transparent; -fx-text-fill: transparent");

			second_line_chart.prefHeight(700);


			second_line_chart.setLegendVisible(false);
			second_line_chart.setAnimated(false);
			second_line_chart.setAlternativeRowFillVisible(false);
			second_line_chart.setAlternativeColumnFillVisible(false);
			second_line_chart.setCreateSymbols(true);
			second_line_chart.setHorizontalGridLinesVisible(false);
			second_line_chart.setVerticalGridLinesVisible(false);
			xAxis_2.setTickMarkVisible(false);
			xAxis_2.setTickLabelsVisible(false);
			second_line_chart.getXAxis().setTickMarkVisible(false);
			second_line_chart.getXAxis().setOpacity(0);
			yAxis.setMaxWidth(30);
			yAxis.setMinWidth(30);
			yAxis.setPrefWidth(30);
			yAxis_2.setMaxWidth(30);
			yAxis_2.setMinWidth(30);
			yAxis_2.setPrefWidth(30);
			xAxis_2.setPrefHeight(76);
			xAxis.setPrefHeight(76);
			((Path)second_line_chart.getXAxis().lookup(".axis-minor-tick-mark")).setVisible(false);

			xAxis_2.setOpacity(0);
			//yAxis_2.setTranslateZ(200);



			if(selected_sensor.size() == 2)
			{
				String current_sensor[] = selected_sensor.get(1).split("-");
				String location = current_sensor[0].replaceAll("\\s+", "");
				String metric = current_sensor[1].replaceAll("\\s+", "");
				Sensor sensor_two = new Sensor(location, metric);
				yAxis_2.setLabel(sensor_two.getMetric());
				lineChart.setTitle(sensor.getLocation() + " " + sensor.getMetric() + "/" + sensor_two.getMetric());
				if(!metric.equals(sensor.getMetric()))
				{
					new_series = drawAnotherLineChart(sensor_two, date_from,date_to,interval);
					second_line_chart.getData().add(new_series);
					List<DataPoint> data_points_two = new_data_series.getDataPoints();
					for (XYChart.Series<Number, Number> s : second_line_chart.getData()) {
						for(int i = 0; i < s.getData().size(); i++)
						{

							DecimalFormat df = new DecimalFormat("#.##");

							Date date = new Date(data_points_two.get(i).getTime());
							String y_label = sensor_two.getLocation() + "/" + sensor_two.getMetric();
							String str = date.toString() + '\n' + y_label + " - " + df.format(s.getData().get(i).getYValue());
							Tooltip hover = new Tooltip(str);
							hover.setShowDelay(Duration.seconds(0));
							Tooltip.install(s.getData().get(i).getNode(), hover);
						}
					}

				}

			}
			else
				lineChart.setTitle(selected_sensor.get(0));
			if(!clientConnection.getMaximised())
			{
				lineChart.setPrefWidth(800);
				second_line_chart.setPrefWidth(800);
			}
			else
			{
				lineChart.setPrefWidth(1500);
				second_line_chart.setPrefWidth(1500);
			}
			StackPane chart = new StackPane();
			chart.setId("chart");
			second_line_chart.setPrefWidth(900);
			chart.setStyle("-fx-background-color: #000000;");
			AnchorPane.setLeftAnchor(chart, 250.0);
			AnchorPane.setRightAnchor(chart, 0.0);
			AnchorPane.setBottomAnchor(chart, 0.0);
			AnchorPane.setTopAnchor(chart, 0.0);
			chart.setAlignment(Pos.CENTER);
			if(new_series != null)
			{
				chart.getChildren().addAll(lineChart,second_line_chart);
			}
			else
				chart.getChildren().add(lineChart);



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
						second_line_chart.setPrefWidth(1500);
						second_line_chart.setPrefHeight(900);
						clientConnection.setMaximised(true);
					}
					else
					{
						lineChart.setPrefWidth(900);
						lineChart.setPrefHeight(900);
						second_line_chart.setPrefHeight(500);
						second_line_chart.setPrefWidth(900);
						clientConnection.setMaximised(false);

					}
				}
			});

		}catch (ExecutionException | InterruptedException e )
		{
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.NONE);
				alert.setAlertType(Alert.AlertType.ERROR);
				alert.setContentText("Disconnected!");
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

	public XYChart.Series drawAnotherLineChart(Sensor sensor, long date_from, long date_to, long interval)
	{
		DataSeries new_data_series = null;
		try {
			new_data_series = clientConnection.queryData(sensor, date_from, date_to, interval).get();
			if(new_data_series.getMinTime() == -1)
			{
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.NONE);
					alert.setAlertType(Alert.AlertType.ERROR);
					alert.setContentText("No data could be found for the selected dates for sensor "
							+ sensor.getLocation() + "/" + sensor.getMetric());
					alert.show();
				});
				return null;
			}



			XYChart.Series series1 = new XYChart.Series();
			series1.setName("Line Chart");
			List<DataPoint> data_points = new_data_series.getDataPoints();
			long inc = 0;
			for (int i = 0; i < data_points.size(); i++) {
				Double value = data_points.get(i).getValue();
				series1.getData().add(new XYChart.Data((double)inc/60000.0, value));
				inc += interval;
			}

			return series1;

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
return null;

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
