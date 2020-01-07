package at.tugraz.oo2.client.ui.controller;

import at.tugraz.oo2.client.ClientConnection;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.*;
import java.util.concurrent.*;

public class LiveDataLineChartUI extends ScrollPane {
    private ScheduledExecutorService executor;
    private final ClientConnection clientConnection;
    public static ScheduledThreadPoolExecutor ses;
    public static Runnable new_runnable;
    public static ScheduledFuture<?> sched_future;
    List<Sensor> sensors_line_chart = null;
    int row_index = 0;
    public LiveDataLineChartUI(ClientConnection clientConnection) {

        this.clientConnection = clientConnection;



        this.getStyleClass().add("sp");
        this.getStylesheets().add("/cluster.css");
        this.getStylesheets().add("/live_data_chart.css");
        this.setFitToWidth(true);
        this.setFitToHeight(true);
        this.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        this.setPannable(true);

        clientConnection.addConnectionOpenedListener(this::onConnectionOpened);
        clientConnection.addConnectionClosedListener(this::onConnectionClosed);


    }

    private void onConnectionOpened()  {


        ProgressBar pb = new ProgressBar();
        Label new_label = new Label("Creating line chart");
        new_label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font: 20px 'Arial'; -fx-padding: 10,0,0,0");
        new_label.setAlignment(Pos.BASELINE_CENTER);
        VBox new_vbox  = new VBox();
        new_vbox.getChildren().add(new_label);
        new_vbox.getChildren().add(pb);
        new_vbox.setAlignment(Pos.CENTER);
        BorderPane new_pane = new BorderPane();
        new_pane.setCenter(new_vbox);
        this.setContent(new_pane);



        ses = new ScheduledThreadPoolExecutor(1);

        new_runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (clientConnection) {

                    List<Sensor> sensors = null;
                    try {
                        sensors = clientConnection.querySensors().get();
                        sensors_line_chart = new ArrayList<>(sensors);
                        createLiveDataLineChart();

                        if(!clientConnection.getRunning())
                        {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    setContent(null);
                                }
                            });
                            return;
                        }

                        if(sensors == null)
                        {
                            sched_future.cancel(true);
                            ses.shutdown();
                            ses.awaitTermination(10, TimeUnit.SECONDS);
                            return;
                        }
                    } catch (InterruptedException e)
                    {
                        System.out.println("Disconnected!");
                        setContent(null);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        ses.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
        sched_future = ses.scheduleAtFixedRate(new_runnable , 0, 10, TimeUnit.SECONDS);



    }

    private void onConnectionClosed()
    {

        try {
            sched_future.cancel(true);
            ses.shutdown();
            ses.awaitTermination(10, TimeUnit.SECONDS);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ses.shutdownNow();

    }
    void createLiveDataLineChart() throws ExecutionException, InterruptedException {
        GridPane new_pane = new GridPane();
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.SOMETIMES);
        col1.setPercentWidth(100);
        new_pane.getColumnConstraints().add(col1);
        HashMap<String, HashMap<String, DataSeries>> metric_location_map = new HashMap<>();
        for(int i = 0; i < sensors_line_chart.size(); i++)
        {

            if(metric_location_map.containsKey(sensors_line_chart.get(i).getMetric()))
            {

                Sensor sensor = new Sensor(sensors_line_chart.get(i).getLocation(), sensors_line_chart.get(i).getMetric());
                DataPoint now = clientConnection.queryValue(sensor).get();
                DataSeries data_series = clientConnection.queryData(sensor,now.getTime()-(50*60*1000), now.getTime(), 5*60*1000).get();
                if(data_series != null) {
                    HashMap<String, DataSeries> get_list = metric_location_map.get(sensors_line_chart.get(i).getMetric());
                    get_list.put(sensors_line_chart.get(i).getLocation(), data_series);
                }
            }
            else
            {


                HashMap<String, DataSeries> new_locations = new HashMap<>();
                Sensor sensor = new Sensor(sensors_line_chart.get(i).getLocation(), sensors_line_chart.get(i).getMetric());
                DataPoint now = clientConnection.queryValue(sensor).get();
                DataSeries data_series = clientConnection.queryData(sensor,now.getTime()-(50*60*1000), now.getTime(), 5*60*1000).get();
                if(data_series != null) {
                    new_locations.put(sensors_line_chart.get(i).getLocation(), data_series);

                    metric_location_map.put(sensors_line_chart.get(i).getMetric(), new_locations);
                }
            }
        }
        row_index = 0;
        for (Map.Entry<String, HashMap<String,DataSeries>> entry : metric_location_map.entrySet()) {

            final CategoryAxis xAxis_2 = new CategoryAxis();
            xAxis_2.setTickLabelRotation(90);
            xAxis_2.setAutoRanging(true);
            final NumberAxis yAxis_2 = new NumberAxis();
            final LineChart<String, Number> lineChart_2 =
                    new LineChart<String, Number>(xAxis_2, yAxis_2);
            lineChart_2.setCreateSymbols(true);
            lineChart_2.setHorizontalGridLinesVisible(false);
            lineChart_2.setVerticalGridLinesVisible(false);
            lineChart_2.getStylesheets().add("/live_data_chart.css");


            lineChart_2.setTitle("Metric " + entry.getKey());
            lineChart_2.setLegendVisible(true);
            HashMap<String, DataSeries> location = entry.getValue();
            for (Map.Entry<String, DataSeries> entry_loc : location.entrySet())
            {

                XYChart.Series series2 = new XYChart.Series();
                series2.setName(entry_loc.getKey());
                long time = System.currentTimeMillis() - 10 * 60 * 1000;

                DataSeries data = entry_loc.getValue();
                for(int i = 0; i < data.getDataPoints().size(); i++)
                {
                    String date = new Date(time).toString();
                    series2.getData().add(new XYChart.Data<>(date, data.getDataPoints().get(i).getValue()));
                    time += 1 * 60 * 1000;

                }

                lineChart_2.getData().add(series2);

            }
            lineChart_2.setPrefHeight(500);

            new_pane.addRow(row_index,lineChart_2);
            row_index++;




        }
        Platform.runLater(() -> {
            this.setFitToHeight(false);

            this.setContent(new_pane);

        });

        new_pane.setStyle("-fx-background-color: black");



    }



}
