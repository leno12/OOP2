package at.tugraz.oo2.server;

import at.tugraz.oo2.data.DataSeries;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Cache {
  HashMap<String, HashMap<String, List<DataSeries>>>cache;
  HashMap<String,DataSeries> second_map;
  private String cache_file = "Cache.txt";
  public Cache()
  {
      cache = new HashMap<>();
      second_map = new HashMap<>();
      File f = new File(cache_file);
      if(f.exists() && !f.isDirectory()) {
          loadCache();
      }
      else
      {
          try {
              f.createNewFile();
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
  }

    public void loadCache()
    {
        BufferedReader reader;
        try {
            String location = "";
            String metric = "";
            long min = 0;
            long max = 0;
            long interval = 0;
            double values[] = null;
            boolean present[] = null;
            reader = new BufferedReader(new FileReader(cache_file));
            String line = reader.readLine();
            while (line != null) {
                if(line.isEmpty())
                    continue;
                String splitted[] = line.split(":");
                String splitted_temp[] = splitted[0].split("\\s+");
                location = splitted_temp[0];
                metric = splitted_temp[1];
                min = Long.parseLong(splitted_temp[2]);
                max = Long.parseLong(splitted_temp[3]);
                interval = Long.parseLong(splitted_temp[4]);
                String values_split[] = splitted[1].split("\\s+");
                values = new double[values_split.length];
                for(int i = 0; i < values_split.length; i++)
                {
                    values[i] = Double.parseDouble(values_split[i]);
                }
                String present_split[] = splitted[2].split("\\s+");
                present =  new boolean[present_split.length];
                for(int i = 0; i < present.length; i++)
                {
                    present[i] = Boolean.parseBoolean(present_split[i]);
                }

                DataSeries new_data_series = new DataSeries(min, max,interval,values,present);
                this.insertNewEntry(location,metric,new_data_series, true);
                line = reader.readLine();

            }
            reader.close();

        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public void insertNewEntry(String location, String metric,DataSeries data_series, boolean startup)
    {
        if(cache.containsKey(location))
        {
            HashMap<String, List<DataSeries>> get_map = cache.get(location);

            if(get_map.containsKey(metric))
            {
                List<DataSeries> data_series2 = get_map.get(metric);
                if(data_series2 == null)
                {
                    data_series2= new ArrayList<DataSeries>();
                    data_series2.add(data_series);
                    if(!startup)
                        this.saveToCache(location,metric,data_series);

                }
                else
                {
                    data_series2.add(data_series);
                    if(!startup)
                       this.saveToCache(location,metric,data_series);


                }

            }
            else
            {
                List<DataSeries> new_list = new ArrayList<>();
                new_list.add(data_series);
                get_map.put(metric, new_list);
                if(!startup)
                    this.saveToCache(location,metric,data_series);


            }
        }
        else
            {
                List<DataSeries> new_list = new ArrayList<>();
                new_list.add(data_series);
                HashMap<String, List<DataSeries>> second_map = new HashMap<>();
                second_map.put(metric, new_list);
                cache.put(location, second_map);
                if(!startup)
                    this.saveToCache(location,metric,data_series);


            }
    }
    public void saveToCache(String location, String metric, DataSeries data_series)
    {
        FileWriter fw = null;
        try {
            fw = new FileWriter(cache_file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fw);
        try {
            bw.write(location + " ");
            bw.write(metric + " ");
            bw.write(data_series.getMinTime() + " ");
            bw.write(data_series.getMaxTime() + " ");
            bw.write(data_series.getInterval() + ":");
            for (double value : data_series.getData()) {
                bw.write(value + " ");
            }
            bw.write(":");
            for (boolean b : data_series.getPresent()) {
                bw.write(b + " ");
            }
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public DataSeries checkIfEntryExists(String location,String metric, long from, long to)
    {
        if(cache.containsKey(location))
        {

            HashMap<String, List<DataSeries>> get_map = cache.get(location);
            if(get_map.containsKey(metric))
            {

                List<DataSeries> data_series2 = get_map.get(metric);

                DataSeries result = null;
                for (int i = 0; i < data_series2.size(); i++) {

                    if(from >= data_series2.get(i).getMinTime() && to <= data_series2.get(i).getMaxTime())
                    {
                        return data_series2.get(i);
                    }
                }
                return result;
            }
        }
        return null;

    }


}
