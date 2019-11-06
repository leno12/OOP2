package at.tugraz.oo2.server;

import at.tugraz.oo2.data.DataSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Cache {
  HashMap<String, HashMap<String, List<DataSeries>>>cache;
  HashMap<String,DataSeries> second_map;

  public Cache()
  {
      cache = new HashMap<>();
      second_map = new HashMap<>();
  }

    public void insertNewEntry(String location, String metric,DataSeries data_series)
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

                }
                else
                {
                    data_series2.add(data_series);
                }

            }
            else
            {
                List<DataSeries> new_list = new ArrayList<>();
                new_list.add(data_series);
                get_map.put(metric, new_list);


            }
        }
        else
            {
                List<DataSeries> new_list = new ArrayList<>();
                new_list.add(data_series);
                HashMap<String, List<DataSeries>> second_map = new HashMap<>();
            second_map.put(metric, new_list);
            cache.put(location, second_map);
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
