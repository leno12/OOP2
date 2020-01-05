package at.tugraz.oo2.server;
import at.tugraz.oo2.Util;
import at.tugraz.oo2.data.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import weka.clusterers.SimpleKMeans;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.lang.Object.*;

import weka.core.*;
import weka.filters.supervised.instance.Resample;

import javax.xml.crypto.Data;

public class RequestHandler extends Thread {
    private final Socket socket;
    private final ObjectOutputStream out_stream;
    private final ObjectInputStream in_stream;
    private final InfluxConnection influx_connection;
    static final String LS_COMMAND = "ls";
    static final String NOW_COMMAND = "now";
    static final String DATA_COMMAND = "data";
    static final String CLUSTER_COMMMAND = "cluster";
    static final String SIM_COMMAND = "sim";

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
                           // System.out.println(new Date(data_series_cluster.subSeries(i,i + interval_clusters).getMinTime()));
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

                        int dimensions = (int)((interval_clusters / (60*1000)) / (interval_points / (60 * 1000)));


                        ArrayList<Attribute> attrList = new ArrayList<Attribute>();
                        for(int i = 0; i < dimensions; i++) {
                            Attribute attr1 = new Attribute("attr" + i);

                            attrList.add(attr1);
                        }

                        Instances dataset = new Instances("test", attrList, 0);


                        for(int i = 0; i < clusters.size(); i++)
                        {

                                Instance instance0 = new DenseInstance(Math.random(), clusters.get(i).getData());
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

                        for (int i = 0; i < clusters.size(); i++) {
                            //System.out.println(MessageFormat.format("point {0} is assigned to cluster {1}", i, assignments[i]));
                            list_of_clusters.get(assignments2[i]).add(clusters.get(i));
                        }
                        List<ClusterDescriptor> cds = new ArrayList<>();

                        for(int i = 0; i < list_of_clusters.size(); i++)
                        {
                            //System.out.println(list_of_clusters.get(i).size());
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

                    case SIM_COMMAND:
                        String metric = (String)command_request.get(1);
                        long start_time = (long)command_request.get(2);
                        long end_time = (long)command_request.get(3);
                        long min_size = (long)command_request.get(4);
                        long max_size = (long)command_request.get(5);
                        int max_result_count = (int)command_request.get(6);
                        double[] ref = (double[])command_request.get(7);

                        List<Sensor> sensor_list = influx_connection.getLocationandMetrics();
                        List<Sensor> sensors_list2 = new ArrayList<>();
                        for(Sensor sen : sensor_list)
                        {
                            if(sen.getMetric().equals(metric))
                            {
                                sensors_list2.add(sen);
                            }
                        }

                        long inc_int = 5*60*1000;
                        List<DataSeries> complete_time_series = new ArrayList<>();
                        SortedMap<Double, MatchedCurve> sm = new TreeMap<Double, MatchedCurve>();
                        DataSeries.normalize(ref);

                        for(Sensor sen : sensors_list2)
                        {
                            Sensor sensor_tmp = sen;
                            DataSeries data_series_complete = influx_connection.getDataSeries(sensor_tmp, start_time, end_time, inc_int, cache);
                            if(data_series_complete == null)
                                continue;

                            DataSeries data_series_sim = data_series_complete.interpolate();
                            DataSeries normalized = data_series_sim.normalize();
                            complete_time_series.add(normalized);
                            if((((max_size/1000)/60) - ((min_size/1000)/60)) > 500)
                            {
                                inc_int = 20*60*1000;
                                while(max_size % inc_int != 0 && min_size % inc_int != 0)
                                    inc_int += 5*60*1000;
                            }

                            for(long scale = min_size; scale <= max_size; scale += inc_int)
                            {
                                long data_num = scale/(5*60*1000);
                                //double[] scaled_ref = new double[(int)data_num];
                                double[] scaled_ref = Util.resize(ref, (int)data_num);
                                scaled_ref = Util.interpolate(scaled_ref);

                                DataSeries.normalize(scaled_ref);
                                for(long i = start_time; i < end_time - scale; i += (5*60*1000))
                                {
                                    DataSeries subseries = normalized.subSeries(i, i + scale);
                                    DataSeries normalized_ss = subseries.normalize();
                                    if(normalized_ss.getPresentValueCount() != scaled_ref.length)
                                        continue;
                                    //System.out.println("Subseries len: " + subseries.getValueCount());
                                    MatchedCurve tmp = new MatchedCurve(sen, normalized_ss, normalized_ss.similarity(scaled_ref));
                                    sm.put(normalized_ss.similarity(scaled_ref), tmp);
                                    subseries = null;
                                    normalized_ss = null;
                                }
                               // System.gc();

                            }
                        }
                        List<MatchedCurve> mc = new ArrayList<>();
                        int count = 0;
                        for(Map.Entry<Double, MatchedCurve> entry : sm.entrySet())
                        {
                            mc.add(entry.getValue());
                            count++;
                            if(count == max_result_count)
                                break;
                        }

                        out_stream.writeObject(mc);
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
