package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.client.ui.component.DurationPicker;
import at.tugraz.oo2.data.ClusterDescriptor;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
	private ScrollPane scrollpane;
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
		Alert alert2 = new Alert(Alert.AlertType.NONE);



		new Thread(() -> {
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
					instant =  localDate.atStartOfDay(ZoneId.of(ZoneId.systemDefault().toString())).toInstant();
					date = Date.from(instant);
					long date_to = date.getTime();
					long interval = spPointsCluster.getValue() * 60 * 1000;
					System.out.println(Long.parseLong(dpIntervalClusters.getValue().toString()) + " here");
					long cluster_interval = Long.parseLong(dpIntervalClusters.getValue().toString()) *  1000;
					int num_of_clusters = spClusters.getValue();
					if(date_from >= date_to)
					{
						Platform.runLater(() -> {
							alert.setAlertType(Alert.AlertType.ERROR);
							alert.setContentText("End Date should be bigger than Start Date");
							alert.show();
							return;
						});
					}
					createClustersscrollPane(sensor, date_from, date_to, interval, selected_sensor,cluster_interval,num_of_clusters);


				} catch (NullPointerException e) {

				}
			}


		}).start();

	}

	public void createClustersscrollPane(Sensor sensor, long date_from, long date_to, long interval, String selected_sensor,
										 long cluster_inerval, int num_of_clusters)
	{
		List<ClusterDescriptor> cds = new ArrayList<>();
		try {
			System.out.println(date_from);
			System.out.println(date_to);
			System.out.println(sensor.getLocation());
			System.out.println(interval);
			System.out.println(cluster_inerval);
			cds = clientConnection.getClustering(sensor, date_from, date_to,cluster_inerval, interval, num_of_clusters).get();
			ObservableList<String> clusters_list = FXCollections.observableArrayList();

			final NumberAxis xAxis = new NumberAxis(date_from,date_to,1000000);
			xAxis.setTickLabelRotation(90);
			final NumberAxis yAxis = new NumberAxis();
			xAxis.setLabel("Interval in minutes");
			final LineChart<Number, Number> lineChart =
					new LineChart<Number, Number>(xAxis, yAxis);

			for (int i = 0; i < cds.size(); i++) {

				String new_sensor = "Cluster #" + (i+1) + "\nError: " + cds.get(i).getClusterError() + "\n" + "" +
						"Number of members: " + cds.get(i).getMembers().size();
				clusters_list.add(new_sensor);
				XYChart.Series series1 = new XYChart.Series();
				for(int j = 0; j < cds.get(i).getMembers().size(); j++)
				{
					for(int z = 0; z < cds.get(i).getMembers().get(j).getData().length; z++) {

						series1.getData().add(new XYChart.Data(cds.get(i).getMembers().get(j).getMinTime(), cds.get(i).getErrorOf(cds.get(i).getMembers().get(j))));

					}

				}

				lineChart.getData().add(series1);

			}
			lineChart.setPrefWidth(1000);
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
					lvClusters.setItems(clusters_list);
					getChildren().add(lineChart);


				}
			});


			lineChart.setCreateSymbols(true);
			lineChart.setTitle(selected_sensor);

			lineChart.setHorizontalGridLinesVisible(false);
			lineChart.setVerticalGridLinesVisible(false);
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
		lvClusters.prefWidthProperty().bind(scrollpane.widthProperty());
		lvClusters.prefHeightProperty().bind(scrollpane.heightProperty());
		this.setStyle("-fx-background-color: black");
		this.getStylesheets().add("/cluster.css");
	}

}
