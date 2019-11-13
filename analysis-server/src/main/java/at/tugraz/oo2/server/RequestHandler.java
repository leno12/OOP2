package at.tugraz.oo2.server;

import at.tugraz.oo2.data.DataPoint;
import at.tugraz.oo2.data.DataSeries;
import at.tugraz.oo2.data.Sensor;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class RequestHandler extends Thread {
    private final Socket socket;
    private final ObjectOutputStream out_stream;
    private final ObjectInputStream in_stream;
    private final InfluxConnection influx_connection;
    static final String LS_COMMAND = "ls";
    static final String NOW_COMMAND = "now";
    static final String DATA_COMMAND = "data";
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

                    default:
                        System.out.println("Error Wrong Command");
                        break;
                }
            } catch (IOException | UnirestException | ClassNotFoundException e) {
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
