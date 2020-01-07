package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.Util;
import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.component.DateTimePicker;
import at.tugraz.oo2.client.ui.component.DurationPicker;
import at.tugraz.oo2.data.ClusterDescriptor;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClusterUI extends HBox {

	private final ClientConnection clientConnection;

	@FXML
	private ListView<String> lvSensors;
	@FXML
	private DatePicker dpFrom;
	@FXML
	private DatePicker dpTo;
	@FXML
	private DurationPicker dpIntervalClusters;
	@FXML
	private Spinner<Integer> spPointsCluster;
	@FXML
	private Spinner<Integer> spClusters;
	@FXML
	private ListView<String> lvClusters;
	@FXML
	private ScrollPane overviewpane;
	@FXML
	private GridPane grid;

	public ClusterUI(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/cluster.fxml"));
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
							String new_sensor = sensors.get(i).getLocation() + " - " + sensors.get(i).getMetric();
							live_data.add(new_sensor);

						}
						Platform.runLater(new Runnable()

						{
							@Override
							public void run()
							{
								lvSensors.setItems(live_data);
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

	/**
	 * Get data needed to draw a linear chart when draw Chart Button is pressed
	 * Create new thread so that GUI stays responsive
	 * @param event
	 */
	@FXML protected void createClusters(ActionEvent event) {

		this.getChildren().remove(this.lookup("#chart"));
		Alert alert = new Alert(Alert.AlertType.NONE);
		if(!checkParameters(lvSensors,null,dpFrom,dpTo))
			return;

		Runnable task = new Runnable()
		{
			public void run() {
				synchronized (clientConnection) {
					try {

						String selected_sensor = lvSensors.getSelectionModel().getSelectedItem();

						String current_sensor[] = selected_sensor.split("-");
						String location = current_sensor[0].replaceAll("\\s+", "");
						String metric = current_sensor[1].replaceAll("\\s+", "");
						Sensor sensor = new Sensor(location, metric);

						LocalDate localDate = dpFrom.getValue();
						Instant instant = localDate.atStartOfDay(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();  //atZone(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();
						Date date = Date.from(instant);
						long date_from = date.getTime();
						localDate = dpTo.getValue();
						instant = localDate.atStartOfDay(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();
						date = Date.from(instant);
						long date_to = date.getTime();
						long interval = spPointsCluster.getValue() * 60 * 1000;
						long cluster_interval = Long.parseLong(dpIntervalClusters.getValue().toString()) * 1000;
						int num_of_clusters = spClusters.getValue();
						if (date_from >= date_to) {
							Platform.runLater(() -> {
								alert.setAlertType(Alert.AlertType.ERROR);
								alert.setContentText("End Date should be bigger than Start Date");
								alert.show();
								return;
							});
						}
						createClustersscrollPane(sensor, date_from, date_to, interval, selected_sensor, cluster_interval, num_of_clusters);


					} catch (NullPointerException e) {

					}
				}
			}

		};
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();



	}

	public void createClustersscrollPane(Sensor sensor, long date_from, long date_to, long interval, String selected_sensor,
										 long cluster_inerval, int num_of_clusters)
	{

		try {


			grid.setVgap(10);
			grid.setHgap(10);






			final List<ClusterDescriptor> cds =  clientConnection.getClustering(sensor, date_from, date_to,cluster_inerval, interval, num_of_clusters).get();
			ObservableList<String> clusters_list = FXCollections.observableArrayList();
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{

					grid.getChildren().clear();
					lvClusters.getItems().clear();
					Label new_label = new Label("Cluster Overview");

					new_label.setMinSize(50,50);
					new_label.setFont(new Font("Arial", 25));
					new_label.setStyle("-fx-text-fill: white");
					GridPane.setHalignment(new_label, HPos.CENTER);

					grid.add(new_label, 0, 0);


				}
			});


           int counter = 1;

			cds.sort(Comparator.comparing(ClusterDescriptor::getClusterError));
			for (int i = 0; i < cds.size(); i++) {

				/*String new_sensor = "Cluster #" + (i + 1) + "\nError: " + cds.get(i).getClusterError() + "\n" + "" +
						"Number of members: " + cds.get(i).getMembers().size();
						*/
				StringBuilder new_cluster = new StringBuilder("Cluster #" + (i + 1) + " with error of " + cds.get(i).getClusterError() + ":" + "\n");
				clusters_list.add(new_cluster.toString());

				for(int c = 0; c < cds.get(i).getMembers().size(); c++) {
					DataSeries member = cds.get(i).getMembers().get(c);
					StringBuilder new_one = new StringBuilder();
					new_one.append("\t\t").append(Util.TIME_FORMAT.format(new Date(member.getMinTime()))).append('\t').append(cds.get(i).getErrorOf(member)).append("\n");
					clusters_list.add(new_one.toString());

				}


				final ClusterDescriptor cluster = cds.get(i);
				XYChart.Series series1 = new XYChart.Series();
				final NumberAxis xAxis = new NumberAxis(0, cds.get(i).getAverage().length, 1);
				xAxis.setTickLabelRotation(90);

				final NumberAxis yAxis = new NumberAxis();
				yAxis.setLabel("Avg value of a dimension");
				final LineChart<Number, Number> lineChart =
						new LineChart<Number, Number>(xAxis, yAxis);
				lineChart.setCreateSymbols(true);
				String title = "Cluster " + (i + 1) + "\n" + "Curves: " + cds.get(i).getMembers().size()
						+  "\n" + "Error: " + cds.get(i).getClusterError();
				lineChart.setTitle(title);
				lineChart.setLegendVisible(false);
				xAxis.setTickLabelsVisible(false);
				yAxis.setTickLabelsVisible(false);
				yAxis.setTickMarkVisible(false);
				xAxis.setTickMarkVisible(false);
				xAxis.setOpacity(0);
				yAxis.setOpacity(0);

				lineChart.setHorizontalGridLinesVisible(false);
				lineChart.setVerticalGridLinesVisible(false);
				for (int j = 0; j < cluster.getAverage().length; j++) {
					series1.getData().add(new XYChart.Data<>(j, cluster.getAverage()[j]));

				}


				lineChart.getStylesheets().add("/chart.css");
				lineChart.getData().add(series1);
				lineChart.setPrefHeight(500);


				final int row_num = counter;
				final int num = i;
				Button button = new Button("View");
				button.getStyleClass().add("draw-button");
				button.setMinSize(150,50);
				GridPane.setHalignment(button, HPos.CENTER); // To align horizontally in the cell
				GridPane.setValignment(button, VPos.CENTER);
				Separator separator1 = new Separator();
				separator1.setStyle("-fx-min-width: 200");

				button.setOnAction(new EventHandler<ActionEvent>() {
					@Override public void handle(ActionEvent e) {

						GridPane grid_tab = new GridPane();
						Label new_label = new Label("Cluster " + (num + 1) + " Detailed view" + "\n" +
								"Error: " + cds.get(num).getClusterError() + "\n" + "Curves: " +
								cds.get(num).getMembers().size());

						new_label.setMinSize(40,40);
						new_label.setFont(new Font("Arial", 20));
						new_label.setStyle("-fx-text-fill: white");
						GridPane.setHalignment(new_label, HPos.LEFT);
						GridPane.setValignment(new_label,VPos.CENTER);

						grid_tab.add(new_label, 0, 0);


						int counter_row = 1;
						int counter_column = 0;

						for(int i = 0; i < cds.get(num).getMembers().size(); i++)
						{

							XYChart.Series series2 = new XYChart.Series();
							final CategoryAxis xAxis_2 = new CategoryAxis();
							xAxis.setTickLabelRotation(90);
							xAxis.setAutoRanging(true);
							final NumberAxis yAxis_2 = new NumberAxis();
							//xAxis.setLabel("Interval in minutes");
							final LineChart<String, Number> lineChart_2 =
									new LineChart<String, Number>(xAxis_2, yAxis_2);
							lineChart_2.setCreateSymbols(true);

							lineChart_2.setHorizontalGridLinesVisible(false);
							lineChart_2.setVerticalGridLinesVisible(false);

							Double error = cds.get(num).getErrorOf(cds.get(num).getMembers().get(i));

							lineChart_2.setTitle("Curve " + (i + 1) + "\n" + "Error: " +  error);
							lineChart_2.setLegendVisible(false);
							for(int j = 0; j < cds.get(num).getMembers().get(i).getDataPoints().size(); j++)
							{
								String date = new Date(cds.get(num).getMembers().get(i).getDataPoints().get(j).getTime()).toString();
								String[] split_date = date.split("CEST");
								series2.getData().add(new XYChart.Data<>(split_date[0],
										cds.get(num).getMembers().get(i).getDataPoints().get(j).getValue()));
							}
							lineChart_2.getStylesheets().add("/chart.css");
							lineChart_2.getData().add(series2);
							lineChart_2.setPrefHeight(500);







							grid_tab.add(lineChart_2,counter_column,counter_row,1,1);


							counter_column++;
							if(counter_column == 2)
							{
								counter_column = 0;
								counter_row++;
							}


						}
						ColumnConstraints col1 = new ColumnConstraints();
						col1.setHgrow(Priority.SOMETIMES);
						col1.setPercentWidth(50);
						col1.setMinWidth(10);
						col1.setPrefWidth(100);

						ColumnConstraints col2 = new ColumnConstraints();
						col2.setHgrow(Priority.SOMETIMES);
						col2.setPercentWidth(50);
						col2.setMinWidth(10);
						col2.setPrefWidth(100);

						grid_tab.getColumnConstraints().addAll(col1,col2);



						Tab new_tab = new Tab("Cluster" + (num + 1));
						AssignmentUI.addTab(new_tab);
						ScrollPane new_scroll_pane = new ScrollPane();
						new_scroll_pane.setStyle("-fx-background-color: black;");
						new_scroll_pane.getStylesheets().add("/cluster.css");
						new_scroll_pane.getStyleClass().add("sp");
						new_scroll_pane.setFitToWidth(true);
						new_scroll_pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
						new_scroll_pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
						new_scroll_pane.setPannable(true);
						new_scroll_pane.setContent(grid_tab);
						grid_tab.setStyle("-fx-background-color: black");
						new_tab.setContent(new_scroll_pane);



					}
				});


				Platform.runLater(new Runnable() {
					@Override
					public void run() {

						grid.addRow(row_num, lineChart);

						grid.addRow(row_num + 1, button);

						grid.addRow(row_num + 2, separator1);

					}
				});
				counter += 3;

			}


			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
					lvClusters.setItems(clusters_list);

				}
			});



			//lineChart.getStylesheets().add("/chart.css");






		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	@FXML
	public void initialize() {
		dpIntervalClusters.setDuration(24*60*60);
		spPointsCluster.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 24, 1));
		spClusters.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 2, 1));
		//lvClusters.prefWidthProperty().bind(scrollpane.widthProperty());
		//lvClusters.prefHeightProperty().bind(scrollpane.heightProperty());
		this.setStyle("-fx-background-color: black");
		overviewpane.setStyle("-fx-background-color: black");
		this.getStylesheets().add("/cluster.css");



	}

	/**
	 * Checks if user input is correct (Sensor, Date, Interval)
	 * @param lvSensors
	 * @param lvSensorsY
	 * @param dpFrom
	 * @param dpTo
	 * @return
	 */
	public static boolean checkParameters(ListView<String> lvSensors, ListView<String> lvSensorsY, DatePicker dpFrom, DatePicker dpTo)
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
		return true;
	}




}
