package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.Util;
import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.GUIMain;
import at.tugraz.oo2.client.ui.component.DateTimePicker;
import at.tugraz.oo2.client.ui.component.DurationPicker;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.MatchedCurve;
import at.tugraz.oo2.data.Sensor;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.canvas.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class SketchUI extends HBox {

	private final ClientConnection clientConnection;
	private TableView sp;

	@FXML
	private ListView<String> lvMetric;
	@FXML
	private DatePicker dpFrom;
	@FXML
	private DatePicker dpTo;
	@FXML
	private DurationPicker dpMinSize;
	@FXML
	private DurationPicker dpMaxSize;
	@FXML
	private Spinner<Integer> spMaxResultCount;

	@FXML
	private VBox vbx;
	@FXML
	private AnchorPane ap;
	@FXML
	private HBox vx;
	@FXML
	private GridPane grid;




	@Data
	public static class LiveData {
		private final String location;
		private final String date;
		private Integer length = null;
		private Double error = null;

	}

	boolean draw = true;
	int count = 0;
	int skip_count = 0;
	int copy_count = 0;
	double[] reference_curve = new double[1000];
	GraphicsContext gc;
	List<MatchedCurve> matches;

	public SketchUI(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/sketch.fxml"));
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
							String new_metric = sensors.get(i).getMetric();
							if(!live_data.contains(sensors.get(i).getMetric()))
							{
								live_data.add(new_metric);

							}


						}
						Platform.runLater(new Runnable()

						{
							@Override
							public void run()
							{
								lvMetric.setItems(live_data);
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

		sp.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				Object selected_sensor = sp.getSelectionModel().getSelectedItem();
				if(mouseEvent.getClickCount() == 2)
				{
					if(sp.getSelectionModel().getSelectedItem() == null)
						return;
					BorderPane border = new BorderPane();
					int index = sp.getItems().indexOf(sp.getSelectionModel().getSelectedItem());
					MatchedCurve selected_match = matches.get(index);
					DataSeries series = selected_match.getSeries();
					String location = selected_match.getSensor().getLocation();
					Double error = selected_match.getError();

					XYChart.Series series2 = new XYChart.Series();
					final CategoryAxis xAxis_2 = new CategoryAxis();
					xAxis_2.setTickLabelRotation(90);
					xAxis_2.setAutoRanging(true);
					final NumberAxis yAxis_2 = new NumberAxis();
					//xAxis.setLabel("Interval in minutes");
					final LineChart<String, Number> lineChart_2 =
							new LineChart<String, Number>(xAxis_2, yAxis_2);
					lineChart_2.setCreateSymbols(true);

					lineChart_2.setHorizontalGridLinesVisible(false);
					lineChart_2.setVerticalGridLinesVisible(false);


					lineChart_2.setTitle("Similarity match " + (index + 1) + " for Metric: " + selected_match.getSensor().getMetric() + "\n" + "Location: " +  selected_match.getSensor().getLocation() + "\n" + "Error: " +  error);
					lineChart_2.setLegendVisible(false);
					int skip = series.getDataPoints().size()/30;
					for(int i = 0; i < series.getDataPoints().size(); ++i)
					{
						if((series.getDataPoints().size() > 30) && (i != 0 && i != series.getDataPoints().size() - 1 && (i % skip) != 0))
							continue;
						String date = new Date(series.getDataPoints().get(i).getTime()).toString();
						String[] split_date = date.split("CEST");
						split_date = split_date[0].split("CET");
						series2.getData().add(new XYChart.Data<>(split_date[0], series.getDataPoints().get(i).getValue()));
					}
					lineChart_2.getStylesheets().add("/chart.css");
					lineChart_2.getData().add(series2);
					lineChart_2.setPrefHeight(500);
					border.setCenter(lineChart_2);

					Tab new_tab = new Tab("Similarity curve" + (index + 1));
					AssignmentUI.addTab(new_tab);
					new_tab.setContent(border);

				}
			}
		});

	}



	@FXML
	public void initialize() {
		reference_curve[0] = -12345;
		dpMinSize.setDuration(60*60);
		dpMaxSize.setDuration(60*60*3);
		spMaxResultCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 16, 5));
		sp = new TableView();
		this.setStyle("-fx-background-color: black");
		sp.setStyle("-fx-background-color: black");
		this.getStylesheets().add("/cluster.css");
		//grid.setStyle("-fx-background-color: green");


		TableColumn<SketchUI.LiveData, String> locationColumn = new TableColumn<>("Location");
		TableColumn<SketchUI.LiveData, String> dateColumn = new TableColumn<>("Date");
		TableColumn<SketchUI.LiveData, String> lengthColumn = new TableColumn<>("Length");
		TableColumn<SketchUI.LiveData, String> errorColumn = new TableColumn<>("Error");


		sp.getColumns().addAll(locationColumn, dateColumn, lengthColumn, errorColumn);

		locationColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("Location"));
		dateColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("Date"));
		lengthColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("Length"));
		errorColumn.setCellValueFactory(new PropertyValueFactory<LiveData, String>("Error"));
		sp.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.sp.getStylesheets().add("/liveui.css");


		Canvas canvas = new Canvas(1000, 400);
		gc = canvas.getGraphicsContext2D();

		gc.setFill(Color.LIGHTCORAL);
		gc.setStroke(Color.LEMONCHIFFON);
		//gc.setLineWidth(5);
		gc.fill();
		gc.strokeRect(0, 0, canvas.getWidth(), canvas.getWidth());
		gc.setFill(Color.RED);
		gc.setStroke(Color.CORNFLOWERBLUE);
		gc.setLineWidth(1);
		gc.setFont(new Font("Arial", 20));


		canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<javafx.scene.input.MouseEvent>() {
			@Override
			public void handle(MouseEvent t) {
				if(draw)
				{
					gc.beginPath();
					gc.moveTo(t.getX(), t.getY());
					reference_curve[count] = t.getY();
					incrementCount();
					gc.stroke();
				}

			}
		});
		canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<javafx.scene.input.MouseEvent>() {
			@Override
			public void handle(MouseEvent t) {
				if(draw)
				{
					gc.lineTo(t.getX(), t.getY());
					//reference_curve[count] = t.getY();
					incrementSkipCount();
					if(checkCount())
					{
						reference_curve[count] = t.getY();
						incrementCount();
					}
					gc.stroke();
				}

			}
		});
		canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<javafx.scene.input.MouseEvent>() {
			@Override
			public void handle(MouseEvent t) {
				if(draw)
				{
					reference_curve[count] = t.getY();
					incrementCount();
					//System.out.println(count);

					DataSeries.normalize(reference_curve);
					resetCount();
				}
			}
		});
		Label new_label = new Label("Draw your reference curve here");
		new_label.setMinSize(40, 40);
		new_label.setFont(new Font("Arial", 20));
		new_label.setStyle("-fx-text-fill: white");
	//	ap.getChildren().add(0, new_label);
		new_label.setLayoutX(200);
		//canvas.setStyle("-fx-background-color: green");
		new_label.setAlignment(Pos.BASELINE_CENTER);
		//refernce curve here", Math.round(canvas.getWidth()/2), Math.round(canvas.getHeight()/2));

		//new_label.setMaxWidth(Double.MAX_VALUE);
		//AnchorPane.setLeftAnchor(new_label, 350.0);
		//AnchorPane.setRightAnchor(new_label, 200.0);
		//vbx.getChildren().add(1, canvas);
		//canvas.widthProperty().bind(ap.widthProperty());
		//vx.getChildren().add(canvas);


		canvas.widthProperty().bind(grid.widthProperty());

		new_label.setAlignment(Pos.CENTER);
		//canvas.heightProperty().bind(vx.heightProperty());
		Separator separator = new Separator(Orientation.HORIZONTAL);
		VBox new_box = new VBox();
		GridPane.setHalignment(new_label, HPos.CENTER);
		new_box.getChildren().add(new_label);
		new_box.getChildren().add(separator);
		new_box.getChildren().add(canvas);
		new_box.setAlignment(Pos.CENTER);


		grid.addRow(0,new_box);
		grid.addRow(1,sp);

  canvas.setStyle("-fx-border-color: red");
  canvas.setStyle("-fx-border-width: 20");
	}
	public void incrementCount()
	{
		count -= -1;
	}

	public void incrementSkipCount()
	{
		skip_count -= -1;
	}
	public void resetCount()
	{
		copy_count = count;
		count -= count;
		skip_count -= skip_count;
		draw = false;
	}

	public boolean checkCount()
	{
		if((skip_count%5) == 0)
			return true;
		return false;
	}
	/**
	 * Get data needed to draw a linear chart when draw Chart Button is pressed
	 * Create new thread so that GUI stays responsive
	 * @param event
	 */
	@FXML protected void getSimilarity(ActionEvent event) {

		Alert alert = new Alert(Alert.AlertType.NONE);

		if(!checkParameters(lvMetric, null, dpFrom, dpTo, dpMinSize, dpMaxSize))
			return;

		new Thread(() -> {
			synchronized (clientConnection) {
				try {

					String selected_metric = lvMetric.getSelectionModel().getSelectedItem();

					LocalDate localDate = dpFrom.getValue();
					Instant instant = localDate.atStartOfDay(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();  //atZone(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();
					Date date = Date.from(instant);
					long date_from = date.getTime();
					localDate = dpTo.getValue();
					instant =  localDate.atStartOfDay(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();
					date = Date.from(instant);
					long date_to = date.getTime();
					long min_size = Long.parseLong(dpMinSize.getValue().toString()) *  1000;
					long max_size = Long.parseLong(dpMaxSize.getValue().toString()) *  1000;
					int max_results = spMaxResultCount.getValue();
					double[] copy_ref = new double[copy_count];
					System.arraycopy(reference_curve, 0, copy_ref, 0, copy_count);
					//ArrayUtils.reverse(copy_ref);
					double[] final_ref = new double[copy_ref.length];
					for(int i = 0; i < copy_ref.length; ++i)
					{
						final_ref[i] = 1.0 - copy_ref[i];
					}


					matches = clientConnection.getSimilarity(selected_metric, date_from, date_to, min_size, max_size, max_results, final_ref).get();

					StringBuilder new_sim = new StringBuilder("Got " + matches.size() + " matched curves:" + "\n");
					ObservableList<LiveData> sim_list = FXCollections.observableArrayList();
					for (int i = 0; i < matches.size(); i++) {
						final MatchedCurve match = matches.get(i);
						final DataSeries series = match.getSeries();
						LiveData sim_obj = new LiveData(match.getSensor().getLocation(), Util.TIME_FORMAT.format(new Date(series.getMinTime())));
						sim_obj.length = Integer.parseInt(String.valueOf(series.getLength() / 1000));
						sim_obj.error = (match.getError());
						sim_list.add(sim_obj);
					}
					Platform.runLater(new Runnable()
					{
						@Override
						public void run()
						{
							sp.setItems(sim_list);

						}
					});






				} catch (NullPointerException e) {

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}


		}).start();


	}
	@FXML protected void clearRef(ActionEvent event) {
		gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
		sp.getItems().clear();
		Arrays.fill(reference_curve, 0.0);
		reference_curve[0] = -12345;
		draw = true;


	}

	public boolean checkParameters(ListView<String> lvSensors, ListView<String> lvSensorsY, DatePicker dpFrom, DatePicker dpTo, DurationPicker min, DurationPicker max)
	{
		Alert alert = new Alert(Alert.AlertType.NONE);
		if(lvSensors.getSelectionModel().isEmpty() || (lvSensorsY != null && lvSensorsY.getSelectionModel().isEmpty()))
		{

			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Please choose one metric");
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

		else if(dpFrom.getValue().isAfter(LocalDate.now()) || dpTo.getValue().isAfter(LocalDate.now()))
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
		else if(min.getValue().longValue() >= max.getValue().longValue())
		{
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("Min size must be smaller than max size");
			alert.show();
			return false;
		}
		else if(reference_curve[0] == -12345) {
			alert.setAlertType(Alert.AlertType.ERROR);
			alert.setContentText("You must draw a reference curve");
			alert.show();
			return false;
		}
		return true;
	}

}
