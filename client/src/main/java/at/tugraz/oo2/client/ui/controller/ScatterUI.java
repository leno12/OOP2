package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.GUIMain;
import at.tugraz.oo2.client.ui.component.DateTimePicker;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.RangeSlider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScatterUI extends HBox {

	private final ClientConnection clientConnection;
	private ProgressBar pb;

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

	@FXML
	private Button scatterHistoryButton;

	public String[] content_lines;



	public ScatterUI(ClientConnection clientConnection) {
		this.pb = null;
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

	/**
	 * Get avalibale sensors from the server and display them in list view
	 */
	private void onConnectionOpened() {
		Label progress_label = new Label("Fetching sensor data");
		HBox new_progress_bar = new HBox();
        if(pb == null) {
        	pb = new ProgressBar();
			createProgressBar(pb, progress_label, new_progress_bar);
		}
		Runnable task = new Runnable()
		{
			public void run()
			{

				synchronized (clientConnection) {

					try {
						if(!clientConnection.getRunning())
						{
							Platform.runLater(new Runnable() {
								@Override
								public void run()
								{
									ap.getChildren().remove(progress_label);
									ap.getChildren().remove(new_progress_bar);
								}
								});

							return;
						}
						List<Sensor> sensors = clientConnection.querySensors().get();
						if(sensors == null)
						{
							Platform.runLater(() -> {
								ap.getChildren().remove(progress_label);
								ap.getChildren().remove(new_progress_bar);
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
								lvSensorX.setItems(live_data);
								lvSensorY.setItems(live_data);
								ap.getChildren().remove(progress_label);
								ap.getChildren().remove(new_progress_bar);
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
		thread.start();

	}

	@FXML
	public void initialize() {

		this.getStylesheets().add("/scatterui.css");
		spInterval.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 10000, 60, 5));



	}

	/**
	 * Get needed data from server to draw a scatter plot
	 * Create new thread so that GUI stays responsive
	 */
	@FXML
	public void drawScatterPlotButton() {
		this.getChildren().remove(this.lookup("#chart"));

		Alert alert = new Alert(Alert.AlertType.NONE);
		Alert alert2 = new Alert(Alert.AlertType.NONE);
		if(!LineChartUI.checkParameters(lvSensorX,lvSensorY,dpFrom,dpTo))
			return;
		Label progress_label = new Label("Creating graph");
		ProgressBar pb = new ProgressBar();
		HBox new_progress_bar = new HBox();
		this.createProgressBar(pb,progress_label,new_progress_bar);

		new Thread(() -> {

			synchronized (clientConnection) {
				try {

					String selected_sensor_x = lvSensorX.getSelectionModel().getSelectedItem();
					String selected_sensor_y = lvSensorY.getSelectionModel().getSelectedItem();
					if(selected_sensor_x.equals(selected_sensor_y))
					{
						Platform.runLater(() -> {
							ap.getChildren().remove(new_progress_bar);
							ap.getChildren().remove(progress_label);
							alert2.setAlertType(Alert.AlertType.ERROR);
							alert2.setContentText("Please choose two distinct sensors");
							alert2.show();

						});
						return;
					}

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
					this.drawScatterPlot(sensor_x,sensor_y ,date_from, date_to, interval);
					Platform.runLater(() -> {
						ap.getChildren().remove(progress_label);
						ap.getChildren().remove(new_progress_bar);
					});



				} catch (NullPointerException e)
				{
					Platform.runLater(() -> {
						ap.getChildren().remove(progress_label);
						ap.getChildren().remove(new_progress_bar);
						alert2.setAlertType(Alert.AlertType.ERROR);
						alert2.setContentText("Disconnected!");
						alert2.show();
					});

				}
			}

		}).start();


	}


	/**
	 * Get data needed to show Scatter query history when Show recent queries Button is pressed
	 * @param event
	 */
	@FXML protected void scatterHistory(ActionEvent event) {


		String hist = "ScatterHistory.txt";
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
					Date date_from = new Date(Long.parseLong(arr[4]));
					Date date_to = new Date(Long.parseLong(arr[5]));
					Integer inter = Integer.parseInt(arr[6]);
					inter /= 60000;
					dpFrom.setDate(date_from.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
					dpTo.setDate(date_to.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
					spInterval.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 10000, inter, 5));
					lvSensorX.getSelectionModel().select(arr[0] + " - " + arr[1]);
					lvSensorY.getSelectionModel().select(arr[2] + " - " + arr[3]);
					dialog.close();
				}
			}
		});
	}

	/**
	 * Draw scatter plot and display it
	 * @param sensor_x
	 * @param sensor_y
	 * @param date_from
	 * @param date_to
	 * @param interval
	 */
	public void drawScatterPlot(Sensor sensor_x,Sensor sensor_y, long date_from, long date_to, long interval)
	{
		try {
			DataSeries new_data_series_x = clientConnection.queryData(sensor_x, date_from, date_to, interval).get();
			DataSeries new_data_series_y = clientConnection.queryData(sensor_y, date_from, date_to, interval).get();
			if(new_data_series_x.getMinTime() == -1 || new_data_series_y.getMinTime() == -1)
			{
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.NONE);
					alert.setAlertType(Alert.AlertType.ERROR);
					alert.setContentText("No data could be found for the selected dates for one of the sensors");
					alert.show();
				});
				return;
			}
			else if(new_data_series_x.getMinTime() == 0 || new_data_series_y.getMinTime() == 0)
			{
				Platform.runLater(() -> {
					Alert alert = new Alert(Alert.AlertType.NONE);
					alert.setAlertType(Alert.AlertType.ERROR);
					alert.setContentText("Can't reach the server");
					alert.show();
				});
				return;
			}

			String scatter_history = "ScatterHistory.txt";
			FileWriter fw = null;
			try {
				fw = new FileWriter(scatter_history, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			BufferedWriter bw = new BufferedWriter(fw);
			String content = " ";
			Boolean todo = true;
			try {
				content = Files.readString(Paths.get(scatter_history), StandardCharsets.UTF_8);
				content_lines = content.split(System.getProperty("line.separator"));
				for(int i = 0; i < content_lines.length; ++i)
				{
					String line_check = sensor_x.getLocation() + " " + sensor_x.getMetric() + " " +
							sensor_y.getLocation() + " " + sensor_y.getMetric() + " " + date_from + " " + date_to +
							" " + interval;
					if(line_check.equals(content_lines[i]))
						todo = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(todo)
			{
				try {
					bw.write(sensor_x.getLocation() + " ");
					bw.write(sensor_x.getMetric() + " ");
					bw.write(sensor_y.getLocation() + " ");
					bw.write(sensor_y.getMetric() + " ");
					bw.write(date_from + " ");
					bw.write(date_to + " ");
					bw.write(interval + "\n");
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}


			List<DataPoint> data_points_x = new_data_series_x.getDataPoints();
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

			for (XYChart.Series<Number, Number> s : scatterChart.getData()) {
				for(int i = 0; i < s.getData().size(); i++)
				{
					Date date = new Date(data_points_x.get(i).getTime());
					DecimalFormat df = new DecimalFormat("#.##");

					String str = date.toString() + '\n' + x_label + " - " + df.format(s.getData().get(i).getXValue()) +
							'\n' + y_label + " - " + df.format(s.getData().get(i).getYValue());
					Tooltip hover = new Tooltip(str);
					hover.setShowDelay(Duration.seconds(0));
					Tooltip.install(s.getData().get(i).getNode(), hover);
				}
			}

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
				alert.setContentText("Disconnected!");
				alert.show();
			});
		}
	}

	/**
	 * Create progress bar so that user knows that graph is being created or sensor data is being fetched
	 * @param pb
	 * @param progress_label
	 * @param new_progress_bar
	 */
	public void createProgressBar(ProgressBar pb, Label progress_label, HBox new_progress_bar) {
		progress_label.setMaxWidth(Double.MAX_VALUE);
		AnchorPane.setLeftAnchor(progress_label, 0.0);
		AnchorPane.setRightAnchor(progress_label, 0.0);
		AnchorPane.setBottomAnchor(progress_label, 0.0);
		AnchorPane.setTopAnchor(progress_label, -80.0);
		progress_label.setAlignment(Pos.CENTER);
		progress_label.setStyle("-fx-text-fill: white; -fx-font-weight:bold; -fx-font: 20px 'Arial'");
		AnchorPane.setLeftAnchor(new_progress_bar, 0.0);
		AnchorPane.setRightAnchor(new_progress_bar, 0.0);
		AnchorPane.setBottomAnchor(new_progress_bar, 0.0);
		AnchorPane.setTopAnchor(new_progress_bar, 0.0);
		new_progress_bar.setAlignment(Pos.CENTER);
		new_progress_bar.getChildren().add(pb);

		ap.getChildren().add(new_progress_bar);
		ap.getChildren().add(progress_label);

	}
}
