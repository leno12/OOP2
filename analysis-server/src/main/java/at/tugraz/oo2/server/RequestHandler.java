package at.tugraz.oo2.server;
import at.tugraz.oo2.data.ClusterDescriptor;
import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import weka.clusterers.SimpleKMeans;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import ca.pjer.ekmeans.EKmeans;
import weka.core.*;

public class RequestHandler extends Thread {
    private final Socket socket;
    private final ObjectOutputStream out_stream;
    private final ObjectInputStream in_stream;
    private final InfluxConnection influx_connection;
    static final String LS_COMMAND = "ls";
    static final String NOW_COMMAND = "now";
    static final String DATA_COMMAND = "data";
    static final String CLUSTER_COMMMAND = "cluster";

    private final Cache cache;
    public RequestHandler(Socket socket, ObjectOutputStream out_stream, ObjectInputStream in_stream,
                          InfluxConnection influxConnection, Cache cache)
    {
        this.socket = socket;
        this.out_stream = out_stream;
        this.in_stream = in_stream;
        this.influx_connection = influxConnection;
        this.cache = cache;

    }


    /**
     *    Handle commands received from the client, If command valid send request to Influxdb to get data
     *       If exit is received close the connection with client
     */
    @Override
    public void run()
    {
        while(true) {
            try {
                List<Object> command_request = (List<Object>) in_stream.readObject();
                String command = (String) command_request.get(0);
                if(command.equals("exit"))
                {
                    this.socket.close();
                    System.out.println("Connection closed");
                    break;
                }
                switch (command) {
                    case LS_COMMAND:
                        List<Sensor> sensor = influx_connection.getLocationandMetrics();
                        out_stream.writeObject(sensor);
                        break;
                    case NOW_COMMAND:
                        Sensor sensor_value = (Sensor) command_request.get(1);
                        DataPoint data_point = influx_connection.getCurrentSensorValue(sensor_value);
                        out_stream.writeObject(data_point);
                        break;
                    case DATA_COMMAND:
                        Sensor sensor1 = (Sensor) command_request.get(1);
                        long from = (long) command_request.get(2);
                        long to = (long) command_request.get(3);
                        long interval = (long) command_request.get(4);
                        DataSeries data_series = cache.checkIfEntryExists(sensor1.getLocation(),
                                sensor1.getMetric(), from, to);
                        if (data_series == null) {

                            data_series = influx_connection.getDataSeries(sensor1, from, to, interval, cache);
                        } else {

                            if (from > data_series.getMinTime() || to < data_series.getMaxTime()) {

                                data_series = data_series.subSeries(from, to);
                                data_series = data_series.scale(interval);

                            } else {
                                data_series = data_series.scale(interval);
                            }
                        }
                        out_stream.writeObject(data_series);
                        break;
                    case CLUSTER_COMMMAND:
                        Sensor cluster_sensor = (Sensor) command_request.get(1);
                        long cluster_from = (long) command_request.get(2);
                        long cluster_to = (long) command_request.get(3);
                        long interval_clusters = (long) command_request.get(4);
                        long interval_points = (long) command_request.get(5);
                        Long num_of_clusters = Long.parseLong(command_request.get(6).toString());
                        DataSeries data_series_cluster = influx_connection.getDataSeries(cluster_sensor, cluster_from, cluster_to, interval_points, cache);
                        data_series_cluster = data_series_cluster.interpolate();
                        List<DataSeries> clusters_temp = new ArrayList<>();
                        for(long i = cluster_from; i < cluster_to; i+=interval_clusters)
                        {
                            if(data_series_cluster.subSeries(i,i + interval_clusters).hasGaps())
                                continue;
                            clusters_temp.add(data_series_cluster.subSeries(i,i + interval_clusters));

                        }
                        List<DataSeries>  clusters = new ArrayList<>();
                        for (DataSeries cluster : clusters_temp) {
                            clusters.add(cluster.normalize());
                        }



                       /* EKmeans eKmeans = new EKmeans(centroids, points);
                        eKmeans.setEqual(true);
                        eKmeans.run();
                        List<List<DataSeries>> list_of_clusters = new ArrayList<>();

                        int[] assignments = eKmeans.getAssignments();
                        for(int i = 0; i < num_of_clusters; i++)
                        {
                              List<DataSeries> new_list = new ArrayList<>();
                              list_of_clusters.add(new_list);
                        }

                        for (int i = 0; i < clusters.size(); i++) {
                            //System.out.println(MessageFormat.format("point {0} is assigned to cluster {1}", i, assignments[i]));
                             list_of_clusters.get(assignments[i]).add(clusters.get(i));
                        }
                        */
                        List<List<DataSeries>> list_of_clusters = new ArrayList<>();
                        for(int i = 0; i < num_of_clusters; i++)
                        {
                            List<DataSeries> new_list = new ArrayList<>();
                            list_of_clusters.add(new_list);
                        }
                        ArrayList<Attribute> attrList = new ArrayList<Attribute>();
                        for(int i = 0; i < 24; i++) {
                            Attribute attr1 = new Attribute("attr" + i);

                            attrList.add(attr1);
                        }

                        Instances dataset = new Instances("test", attrList, 0);

                        for(int i = 0; i < clusters.size(); i++)
                        {


                                Instance instance0 = new DenseInstance(1.0, clusters.get(i).getData());
                                instance0.setDataset(dataset);
                                dataset.add(instance0);

                        }



                        SimpleKMeans kmeans = new SimpleKMeans();

                        kmeans.setPreserveInstancesOrder(true);
                        kmeans.setNumClusters(Integer.parseInt(num_of_clusters.toString()));
                        kmeans.setSeed(Integer.parseInt(num_of_clusters.toString()));
                        kmeans.setDontReplaceMissingValues(true);
                        kmeans.buildClusterer(dataset);
                        kmeans.setMaxIterations(10);
                        Instances instances = kmeans.getClusterCentroids();
                        int assignments2[] = kmeans.getAssignments();
                        System.out.println(assignments2.length);

                        for (int i = 0; i < clusters.size(); i++) {
                            //System.out.println(MessageFormat.format("point {0} is assigned to cluster {1}", i, assignments[i]));
                            list_of_clusters.get(assignments2[i]).add(clusters.get(i));
                        }
                        List<ClusterDescriptor> cds = new ArrayList<>();

                        for(int i = 0; i < list_of_clusters.size(); i++)
                        {
                            System.out.println(list_of_clusters.get(i).size());
                            double average[] = new double[24];

                            for(int j = 0; j < list_of_clusters.get(i).size(); j++)
                            {

                                for(int z = 0; z < list_of_clusters.get(i).get(j).getData().length; z++)
                                {
                                    average[z] += list_of_clusters.get(i).get(j).getData()[z];

                                }

                            }
                            for(int s = 0; s < average.length; s++)
                            {
                                average[s] /= list_of_clusters.get(i).size();
                            }
                            ClusterDescriptor new_cd = new ClusterDescriptor(average, list_of_clusters.get(i));
                            cds.add(new_cd);
                        }





                        out_stream.writeObject(cds);


                        break;


                    default:
                        System.out.println("Error Wrong Command");
                        break;
                }
            } catch (IOException | UnirestException | ClassNotFoundException e) {

               //e.printStackTrace();
                try {
                    this.socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println("Connection closed");
                break;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try
        {
            this.out_stream.close();
            this.in_stream.close();

        }catch(IOException e){
            e.printStackTrace();
        }

    }
}
