## [Click for Assignment Description](docs/README.md)
Here is the description of the things that this program does.
# OOP2 CLI AND GUI

OOP2 is a program which is used to display data from all sensors available at TU Graz. It can be used either es CLI or GUI. In GUI user can also display data either as Line Chart or Scatter Plot.

## IMPLEMENTED CLI COMMANDS

- LS - returns list of all available sensors

- NOW - command returns the latest value measured by the wanted sensor

- DATA- returns series of values measured by the wanted sensor in the given time for the given interval

- CACHE- Saves executed queries to Cache and for every new query checks if that query already exists in cache if yes return result from cache no need to get data from Belinda again. We made our own class Cache without API. We saved every entry into HASHMAP.

## IMPLEMENTED GUI COMMANDS
- HOW TO USE GUI - When user clicks the connect button there are progress labels to notify the user that data is being fetched. After a few seconds when GUI is finished with fetching you can start use GUI. Commands available will be explained in the next section. But there are three tabs and every tab contains one possibility. Also when user wants to draw some graph there are progress labels to notify user that graph is being created. And user can go through different tabs and watch live data or try to draw another type of graph. GUI is always responsive. 

- LIVE DATA UI - Gets latest data from all sensors and displays it in a TableView. Data is being refreshed every 10 seconds. We used ScheduledThreadPoolExecutor to refresh the data every 10 seconds.
 
- LINE CHART - User can select one sensor, date, time and interval in which he wants to see data as a chart and than clicks Draw Chart Button. GUI  gets data from the server and displays them as a line chart. X axis is interval starting from 0 and y axis is the value of the selected sensor at the given time.

- SCATTERPLOT - User selects two sensors, date, time and interval and clicks on the Draw Scatter Plot Button. GUI gets data from server and displays them as a Scatter Plot. X axis is the value of the first sensor Y axis is the value of the second sensor.


## BONUS TASK

- WRITING CACHED DATA TO DISK - We also implemented saving of the cache into file. Cache is being saved to files so every time you run the server he looks if there is a cache file and if one exists it loads the cache into RAM. If the file doesn't exists it creates a new one. Also every time new entry is inserted into the cache its also being written to the file.

- HISTORY OF RECENT QUERIES - Every time user executes a query it is being written to the file. And there is a button Show recent queries. If user clicks the button a popup shows with the list of recent queries. In order to select one query user double clicks on one of the queries and then the input fields are being loaded with that query and user can than just click the button to execute that query.


## MORE REMARKS
- We also invested a lot of time in design of the GUI. We made changes almost in every part of the GUI using css so it looks more beautiful. We also made in Scatter Plot and Line chart that user can see current time and values of the point on the hover of the mouse which is really pretty nice feature and helps user to get even better look into data. 
Regarding synchronization. In our cased we used first come first serve principle. So user can in the same time execute more queries. But the first one executed gets first the result. 

