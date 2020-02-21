## [Click for Assignment Description](docs/README.md)

# ASSIGNMENT 1

## OOP2 CLI AND GUI

OOP2 is a program which is used to display data from all sensors available at TU Graz. It can be used either es CLI or GUI. In GUI user can also display data either as Line Chart or Scatter Plot.

### IMPLEMENTED CLI COMMANDS

- LS - returns list of all available sensors

- NOW - command returns the latest value measured by the wanted sensor

- DATA- returns series of values measured by the wanted sensor in the given time for the given interval

- CACHE- Saves executed queries to Cache and for every new query checks if that query already exists in cache if yes return result from cache no need to get data from Belinda again. We made our own class Cache without API. We saved every entry into HASHMAP.

### IMPLEMENTED GUI COMMANDS
- HOW TO USE GUI - When user clicks the connect button there are progress labels to notify the user that data is being fetched. After a few seconds when GUI is finished with fetching you can start use GUI. Commands available will be explained in the next section. But there are three tabs and every tab contains one possibility. Also when user wants to draw some graph there are progress labels to notify user that graph is being created. And user can go through different tabs and watch live data or try to draw another type of graph. GUI is always responsive. 

- LIVE DATA UI - Gets latest data from all sensors and displays it in a TableView. Data is being refreshed every 10 seconds. We used ScheduledThreadPoolExecutor to refresh the data every 10 seconds.
 
- LINE CHART - User can select one sensor, date, time and interval in which he wants to see data as a chart and than clicks Draw Chart Button. GUI  gets data from the server and displays them as a line chart. X axis is interval starting from 0 and y axis is the value of the selected sensor at the given time.

- SCATTERPLOT - User selects two sensors, date, time and interval and clicks on the Draw Scatter Plot Button. GUI gets data from server and displays them as a Scatter Plot. X axis is the value of the first sensor Y axis is the value of the second sensor.


### BONUS TASKS

- WRITING CACHED DATA TO DISK - We also implemented saving of the cache into file. Cache is being saved to files so every time you run the server he looks if there is a cache file and if one exists it loads the cache into RAM. If the file doesn't exists it creates a new one. Also every time new entry is inserted into the cache its also being written to the file.

- HISTORY OF RECENT QUERIES - Every time user executes a query it is being written to the file. And there is a button Show recent queries. If user clicks the button a popup shows with the list of recent queries. In order to select one query user double clicks on one of the queries and then the input fields are being loaded with that query and user can than just click the button to execute that query.


### MORE REMARKS
- We also invested a lot of time in design of the GUI. We made changes almost in every part of the GUI using css so it looks more beautiful. We also made in Scatter Plot and Line chart that user can see current time and values of the point on the hover of the mouse which is really pretty nice feature and helps user to get even better look into data. GUI is always responsive and there are also progress labels so that user knows what is hapenning.
There are also a lot of edge cases covered. Alerts for every possible wrong input or when server cannot be reached.
 
 
# ASSIGNMENT 2

## OOP2 CLUSTERING AND SIMILARITY SEARCH

The main part of the second assignment consisted in the implementation of clustering and similarity search.

### IMPLEMENTED CLI COMMANDS

- CLUSTER - Takes a set of data points, and classifies each data point into a specific cluster. Data points of the same cluster are similiar to each other.

- SIM - Performs a sliding window similarity search over all available sensors according to the given metric and reference curve. The result of this operation are matches that should resemble the given reference curve.

### VISUALISATION WITH GUI

- CLUSTER VISUALISATION- The user selects a sensor, a time interval, the cluster interval (specifies the timespan of one cluster member), points per cluster, the amount of clusters and clicks the "Create Clusters" button. The GUI fetches the required data, and presents the user with a list of all resulting clusters on the left side, and their individual line charts on the right. The clusters are sorted according to their error, from lowest to highest. If the user wants a more detailed view of a cluster, with all its curves, he can click the "View" button underneath the line chart of the desired cluster. Once he does that, a new tab gets opened, which consists of line charts of each curve for the selected cluster.

- SKETCH SEARCH - The user selects a metric, a time interval, a minimum and maximum windowsize (used to scale the reference curve in the sliding window similarity search), a number of best results he wants displayed and then he draws a reference curve in the specified area. After the user clicks the "Compute similarities" button, the GUI fetches the required data, and displays the results in a TableView. Once again, the results are sorted according to their error, from lowest to highest. If the user wants to observe a match as a line chart, he can double click on the desired match in the table, which opens a new tab, that consists of a line chart for the selected match. If the user wants to redraw the reference curve he can click the "Clear Reference Curve Button", which clears the previously drawn reference curve, and makes the canvas available for redrawing.

### BONUS TASKS

- NON MAXIMA SUPPRESSION FOR SIMILARITY SEARCH - Checks the similarity matches for overlaping. If two matches overlap with each other, removes the match with the higher error.

- LIVE DATA AS LINE CHART - Displays the live data in the form of a line chart, with one line chart per metric. The line charts get updated every few seconds. This feature is displayed in its own tab, so if the user likes the more simple TableView approach from before, he can still access the old Live Data TableView display.

- LINE CHART WITH MULTIPLE SENSORS - Expands the functionality of the line chart from the first assignment. Intead of selecting only one sensor, a user can select multiple sensors. In order to select more than one sensor, the user must hold the CTRL key while selecting the sensors.

- CSS - Over the course of both assignments, a lot of work was spent in redesigning the look and feel of the GUI, particularly with the use of CSS. Since we didn't get any bonus points for this in the first assignment, we would greatly appreciate it, if we get some for the second. 

