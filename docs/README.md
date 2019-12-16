# Overview
The goal of this course is to get familiar with advanced object oriented concepts by implementing a system for processing and visualizing real world sensor data.

The ecosystem you'll have to work in is comprised of four components:

* Sensors hidden across the campus measuring temperature, humidity, light, co2 etc. in real time. We can happily assist you with setting up your own sensors to contribute to the data set.
* InfluxDb is the central database where all historic sensor data is stored. Sensors send their data to InfluxDb in regular intervals. Users can make simple queries to retrieve raw data from InfluxDb.
* An Analysis Server takes requests from one or more clients that are connected to it and queries data from InfluxDb, which is then processed and sent to the requesting client for visualization.
* Clients consists of a graphical user interface (GUI) that allows the users to explore and visualize sensor data in an intuitive way.

Throughout this course, you will implement the latter two components using the Java 11 programming language along with a simple Framework being provided by us.

# Timeline
* **Assignment 0** (no due date, no points) 
    * Reading the assignment description, setting up your workspace and getting ready.
* **Assignment 1** (Deadline 17.11.2019, 23:59, 50 points + 10 bonus points)
    * First functional version of the Client/GUI and Analysis Server. Retrieval of raw data from InfluxDb through the Analysis Server and simple visualizations.
* **Assignment 2** (Deadline 06.01.2020, 23:59, 50 points + 10 bonus points)
    * Advanced visualizations such as clustering or similarity search. Extension of the communication protocol to support distributed computation.
* **Optional** Setting up your own data sensor
    * Interested students are supported to set up own sensors to contribute to the test data environment. To this end, we give out a limited number of sensors for interested students, based on a first-come-first-serve basis. The sensors can be kept by students, given that actual data is contributed. Please note this is a optional step and will not influence the course grading. For details, please contact oop2ku-2019@cgv.tugraz.at.

# Assignment 0 - Setting up Your Workspace
This assignment has no due date and won't be graded, but you should nevertheless get it done ASAP and contact us in case you encounter difficulties.

## Team Registration
By the time you read this, you should have already formed teams of up to 4 students. The teams stay the same throughout the course. Please tell us if you still couldn't find a team or if a teammate drops out.

## System Requirements
You should have the following installed on your machine:

* Git
* Java 11
* Maven
* A Java IDE such as Eclipse, IntelliJ or NetBeans
* Download more RAM

Don't underestimate the effort it takes to *correctly* upgrade to Java 11 if you already use an earlier Java version!

## Checking out the Framework
By now you should have been granted access to a git repository designated to your team. All your progress/commits must be documented in this repository, so you aren't allowed to use SVN or another Git service. Our GitLab instance can be found at [https://student.cgv.tugraz.at/](https://student.cgv.tugraz.at/). To sign in, click "Sign in with TUGRAZ online (Students)". Either use SSH (recommended) for authenticating yourself in Git or HTTPS. For the former you'll need to generate an SSH keypair (if you haven't already) and add it to your [GitLab Profile](https://student.cgv.tugraz.at/profile/keys).

Open a shell on your machine and issue the following commands:
```bash
# Clone your team repository, NOT the framework.
git clone git@student.cgv.tugraz.at:oop2_2019/XXX.git # [where XXX is your Assignment Team number]
cd XXX

# Set your Git credentials. Use "--global" instead of "--local" if you
# use Git exclusively at TUGraz.
git config --local user.name "Max Muster"
git config --local user.email "max.muster@student.tugraz.at"

# Add framework as second remote repository.
git remote add framework git@student.cgv.tugraz.at:OOP2.2019/framework.git
git pull framework

# Push the framework to your own remote repository.
# This counts as the first partial contribution and results in getting a grade.
git push -u origin master
```

All other team members can access the team repository with: 
```bash
# Clone your team repository, NOT the framework.
git clone git@student.cgv.tugraz.at:oop2_2019/XXX.git # [where XXX is your Assignment Team number]
cd XXX

# Set your Git credentials. Use "--global" instead of "--local" if you
# use Git exclusively at TUGraz.
git config --local user.name "Max Muster"
git config --local user.email "max.muster@student.tugraz.at"
```

If everything has worked so far, you should see the contents of the framework in your team repository in GitLab.

To pull changes from the framework, issue the command `git pull framework`. Changes to the framework will be announced in the News Group `tu-graz.lv.oop2`.

## Starting the System
The root directory of your repository contains a variety of scripts. Find out what they do and adapt them to your needs by opening them in a text editor before executing them! We recommend to run and build your project from the command line, since we'll do the same when grading.

* `mvn clean install` - Build the project. The resulting build artifacts are located in the directories `./analysis-server/target` and `./client/target`.

* `./quickmvn` - Builds the project and starts the Analysis Server. Is faster than the command above, but less reliable. Use `mvn clean install` especially when making changes to `pom.xml` or when modifying resource files.

* `./server` - Starts the Analysis Server with arguments defined in this script.

* `./cli` - Connects to the Analysis Server and starts the interactive Command Line Interface (CLI).

* `./cli <COMMAND> <ARGS>` - Connects to the Analysis Server and performs one command.

* `./cli < queries/<QUERY FILE>` - Executes a set of pre-defined queries. Useful for debugging.

* `./gui` - Starts the GUI.

Try running these commands (especially `./gui`) to check if you can build the project and if you have installed all components correctly.

## Setting up Your IDE
We provide support for both Eclipse (Thomas) and IntelliJ (Robert, Benedikt). Which IDE you should use depends on your personal preferences.

### Eclipse
Go to File -> Import -> Maven -> Existing Maven Projects -> Next and select the root of your repository as "Root Directory". Then click "Finish". You should see 4 new entries in the Package Explorer:

* analysis-server
* client
* shared
* oo2

The last one merely represents the maven container of the first three.

For each of the first three projects you should configure the following in *Right Click* -> Properties:

* Java Compiler -> Select Java Version 11
* Java Build Path -> Libraries -> JRE System Library -> Edit -> Select Java 11
* Java Editor -> Save Actions -> Configure Workspace Settings ... -> Check "Perform the selected actions on save", "Format source code", "Format edited lines", "Organize Imports", "Additional actions"
* Java Code Style -> Formatter -> Configure Workspace Settings ... -> Create a formatter profile in consistence with the code style of the framework

Now select the `oo2` project and click *Green Play Button* -> Run As -> Maven build -> Goals: "clean install" -> Apply, Run. If you have everything set up correctly, there shouldn't be any build errors and the current branch should be displayed next to the project names.

Eclipse also offers a convenient Git GUI. To see the commit history, *Right Click Project* -> Team -> Show in History. Select "Show All Branches and Tags", which is represented by an icon on the top right of the commit list.

### IntelliJ
We also recommend the Jetbrains IntelliJ IDE for develpment. You can get this IDE for free by applying at [https://www.jetbrains.com/shop/eform/students](https://www.jetbrains.com/shop/eform/students). Install IntelliJ IDEA Ultimate with the default settings (make sure that Maven is installed alongside IntelliJ).

Using Jetbrains IntelliJ, click on *Open*, navigate to the root of your repository, select *pom.xml*, and click *ok*. You should be prompted whether you want to open this file as a project. Do so. For the Project settings, select Java 11 as the language level and configure JDK11 for your project. Using the maven targets *clean* and *install* you can build the project.

The framework is using Project Lombok to generate getters and setters for data classes. It will work without any additional setup on your side, but IntelliJ will show errors (which can be ignored) if you don't install the `Lombok` plugin.  
For a quick overview of what Lombok does check out https://projectlombok.org/

## InfluxDb
Familiarize yourself with the API that Influx offers and how to query data from it. Information on the API is provided at [https://docs.influxdata.com/](https://docs.influxdata.com/). Visit our instance of Grafana at [https://belinda.cgv.tugraz.at/grafana/](https://belinda.cgv.tugraz.at/grafana/) to explore the data set.

The API of InfludDb can be reached via [https://belinda.cgv.tugraz.at/influxdb/query](https://belinda.cgv.tugraz.at/influxdb/query). To be able to read any data through the API, you need to authenticate yourself as the user `oo2ro` using the password `ier9ieYaiJei`. The main database is called `oo2`. To get a list of all sensors on the command line, execute the command:
```
curl -i -XPOST "https://belinda.cgv.tugraz.at/influxdb/query?db=oo2&u=oo2ro&p=ier9ieYaiJei" --data-binary "q=SHOW SERIES on oo2"
```

For easier local testing you can download a backup of the influx database here: [https://belinda.cgv.tugraz.at/influx_backup.zip](https://belinda.cgv.tugraz.at/influx_backup.zip).

With the following command you can restore the database to a running influxDB instance (influx_backup_path should be the path to the extracted zip archive):
```
influxd restore -portable -db oo2 influx_backup_path
```

## When You're Done ...
... with setting everything up, help your teammates with doing the same and then get some sleep. If you encounter any problems, contact us ASAP.

# Framework
The framework is composed of a Maven container holding 3 child projects with different purposes. Both **analysis-server** and **client** depend on **shared**, which is designated for shared data classes such as DataSeries, DataPoint, utilities or packages. During the build process, a non-executable JAR is created from **shared**, which has no use for you. The other two projects are exactly what their name says. **analysis-server** and **client** compile to executable JARs and hold all business logic used either by the Analysis Server or the Client. Do not introduce additional dependencies between those 3 projects. If a piece of code is needed by both Analysis Server and Client, put it in **shared**. Many useful libraries are already included in **pom.xml**. In case you want to use other third party libraries, ask for our permission.

Do not change the general file structure given in the framework. I.e. do not move the root of the maven container and do not rename projects or change their version. You are free however to add new packages or source files as long as your submission compiles and fulfills its purpose.

## Shared
The **shared** projects contains a variety of helpful classes. `Util` contains mostly code for parsing and formatting timestamps. The immutable class `Sensor` represents the identifier for a sensor, which is described by the location of the sensor and a metric (what kind of value it reads). `DataPoint` holds a single sensor reading, consisting of a timestamp and a value.

The class `DataSeries` represents a time series of sensor data. Instead of being implemented as a `List<DataPoint>`, it has a more complex, but efficient structure. A data series has a start time (inclusive), an end time (exclusive) and a number of values spaced within a given interval. The time series might have gaps in it, where no sensor reading is present. The end time can be calculated from the start time and the length of the value array. For example, if the series starts at `2019-10-01 00:00:00`, has 24 values and an interval of 60 minutes, the end time is `2019-10-02 00:00:00`. The value at `data[12]` and `present[12]` contains information about `2019-10-01 12:00:00`. If `present[i]` is true, then there is a valid sensor reading at `data[i]`. If it is false however, the value at `data[i]` is undefined and a reading is absent. This means that a data series may as well contain no sensor data at all.

## Analysis Server
The entry point of **analysis-server** is the method `at.tugraz.oo2.server.ServerMain.main(String... args)`. The program takes exactly 5 arguments:

* `<influx url>` - The HTTP-URL under which the API of InfluxDb can be reached.
* `<influx database name>` - The database in InfluxDb where all data is stored.
* `<influx user name>` - The user to connect to InfluxDb.
* `<influx password>` - The password to connect to InfluxDb. 
* `<port>` - The port the Analysis Server should listen to for incoming connections from clients.

The script `./server` already handles these arguments and starts the server. The server should run indefinitely and wait for incoming requests. In the unmodified framework however, the program prints the parsed arguments and terminates. Thus 99% of the server logic must be implemented by you.

## Client
The main method of **client** is located at `at.tugraz.oo2.client.ClientMain.main(String... args)` and contrary to the Analysis Server, a significant portion of the Client is already implemented. There are three distinct ways the client can function:

* No arguments - Starts a simple GUI that prompts the user to enter the URL of the Analysis Server. The user can then explore the data set visually. The script `./gui` handles this scenario as well as the correct initialization of JavaFX. The basic GUI functionality is already given. Only the business logic and data visualization is up to you.
* `<ip> <port>` - Connects to the Analysis Server and starts the CLI. Done by the script `./cli`.
* `<ip> <port> <command> <args ...>` - Connects to the Analysis Server, executes one single request and terminates. Also done by `./cli`.

All command I/O is already implemented by the class `CommandHandler`. The class `ClientConnection` is responsible for maintaining a connection to the AnalysisServer and for sending and receiving requests. It has most parts missing and thus has to be implemented by you. GUI structure and design is located in the directory `./client/src/main/resources` in the form of *.fxml* files, while GUI logic is implemented by the classes in the package `at.tugraz.oo2.client.ui`. Connection handling and data visualization is yet missing. You can also reuse `ClientConnection` when working on the GUI.

## Client Commands
The CLIs sole purpose is to improve testability for us and to provide better debugging functionality to you. A variety of commands and queries are handled by it, which will be useful to you when working on the GUI:

* `help` - Displays a list of client commands, so you don't have to look it up here.
* `ls` - Displays a list of all sensors (identified by their location and metric) known to InfluxDb.
* `now <location> <metric>` - Queries the last reading of the specified sensor.
* `data <location> <metric> <from-time> <to-time> <interval-minutes>` - Queries sensor data within the given range and interval. While *from* is inclusive, *to* is exclusive. For example, the query `data HSi12 temperature 2019-10-01 2019-10-04 1440` should return 3 data points at `2019-10-01 00:00:00`, `2019-10-02 00:00:00` and `2019-10-03 00:00:00`.
* `cluster <location> <metric> <from-time> <to-time> <intervalClusters-minutes> <intervalPoints-minutes> <numClusters>` - Fetches sensor data within the given time range and divides the data into clusters. `intervalClusters` specifies which timespan one cluster member comprises, while `intervalPoints` influences how many data values one cluster member consists of. `numClusters` dictates how many clusters the final result should have. For example, `... 1440 60 4` implies that each cluster member represents one day containing 24 samples. All days are then structured into 4 clusters. A list of clusters including their average point, members and error are returned.
* `sim <metric> <from-time> <to-time> <minSize-minutes> <maxSize-minutes> <maxResultCount> <ref ...>` - Fetches data from all sensors matching the given metric within the given time range and performs a distributed sliding window similarity search on it. A reference curve is given in `ref`, which is horizontally scaled between `minSize` and `maxSize` and matched with all fetched data. The `maxResultCount` most similar results are returned along with their similarity score.

More information on how these queries should be implemented can be found below.

# General Remarks to Both Assignments
This document specifies the goals of the assignments and gives hints on how to implement solutions. It is deliberately not intended by us to completely define all specifics of the implementation. Students are required to do their own design decisions where needed, and this is actually one of the learning goals of the assignments. Assignment 2 builds upon Assignment 1, so you must use your code from Assignment 1 as a starting point for Assignment 2.

Aside from implementing the features described below, both submissions must fulfill a number of requirements:

* The system must be robust against introduction of new sensors, outages of sensors, data gaps or unreachability of InfluxDb.
* The system shall be correct, which includes correct synchronization, error handling (timeouts!), detection of faulty data/input and closing of streams/connections to avoid resource leaks. The user/client must always get feedback when an error occurs! Error messages must be easy to follow. For example, "Can't connect to server." is better than `java.net.ConnectException: Connection refused` or "Please enter a positive number." is better than `java.lang.NumberFormatException: For input string: "four"`.
* The CLI shipped with the framework must not be altered in its functionality, since we use it to test parts of your submissions.
* Client and Analysis Server shall communicate via Sockets. However it is up to you how you design the protocol and whether you make it stateless or stateful.
* The Analysis Server performs all operations to get the data into final shape as requested by the Client. This means all computations must be performed at the Analysis Server (e.g., caching, clustering, searching).
* The GUI must always be responsive, even when one or more requests are processed in the background.

When your submission is graded, the last commit before the deadline on the **master** branch is checked out. Don't forget to write the file `
md` in the root directory of your repository, where you document important design decisions, open issues or bonus tasks you implemented.

# Assignment 1 - Protocol, Caching, Simple Visualizations
In this assignment you will implement the logic necessary for the Client and Analysis Server to exchange data. You need to integrate a graph library into the already existing GUI to display line charts and scatter plots. To reduce the number of requests to InfluxDb, the Analysis Server must cache already fetched data.

The first assignment is comprised of these tasks, yielding 50 points in total:

## Connection Handling and Protocol (No Points)
Implement the infrastructure that allows one or more clients to connect to the Analysis Server at the same time. Connected clients can send an arbitrary number of requests to the Analysis Server, where they are processed in parallel. Clients can connect without any authentication. Make sure to find a solution for queries that get answered in a different order than they were requested in. Since you need to implement one or more commands to show that your infrastructure works, we can't give you points for this task directly. We will however deduct points if your code lacks synchronization, basic security checks or doesn't handle errors.

You are obligated to use Java Sockets to implement the infrastructure. We recommend using serialization for encoding data, but you can design your own protocol if you want to do it the hard way.

## `ls` Command (**6P**)
The Client sends a request to the Analysis Server with no parameters given. The Analysis Server shall then query all known sensors (consisting of location and metric) from InfluxDb and send the list of sensors back to the Client, which then displays them in the CLI.

## `now` Command (**6P**)
The Client sends a request containing a `Sensor` instance to the Analysis Server. The Analysis Server shall then query the latest value of the given sensor from InfluxDb and send the resulting `DataPoint` back to the Client.

## `data` Command (**10P**)
The Client sends a request containing a `Sensor` instance, a start (inclusive) and end time (exclusive) and an interval to the Analysis Server. The Analysis Server shall query the data from InfluxDb and respond with a `DataSeries` instance to the Client. The resulting data series should have the same length as given in the request, even if there isn't any data present in that time frame.

## Caching (**8P**)
The goal of this task is to let the Analysis Server cache sensor data from previous requests in the RAM, so no more requests to InfluxDb have to be made for similar queries. For example, the query `data ...  2019-10-01 2019-10-05 1440` is processed by first checking if all data in the requested interval is cached. If that's the case, send the cached values to the Client. Otherwise delegate the request to InfluxDb, write the response to the cache and send the result to the Client. Afterwards, the query `data ... 2019-10-01 2019-10-03 60` must result in a cache-hit, while `data ... 2019-10-03 2019-10-07 360` results in a cache-miss.

As you may have noticed, one half of the last query is cached while the other half isn't. It's up to you whether you query the whole range from influx or only the portions that aren't yet cached.

Since intervals are not always the same, we need to agree on a standardized interval for the cache to work with. We choose 5 minutes, as defined by the constant `Util.EPOCH`. Thus *all* requests to InfluxDb must have `Util.EPOCH` set as their interval, no matter the size. When sending cached data to the client, it is resampled to the requested interval. The method `DataSeries.scale(long newInterval)` already does that job, albeit poorly. You are free to, but not obligated to implement a better function. The start and end time parameters are rounded to the nearest value divisible by `Util.EPOCH`. Sensor data can be cached indefinitely, since it won't change and "the ink is already dried up".

Caching the list of sensors and the latest sensor value can be implemented as a bonus task, as described below.

## Connection Handling in the GUI (No Points)
Either use the GUI skeleton provided by the framework or develop your own GUI from scratch. You can make any design changes as long as your GUI is easy to use and fulfills the requirements listed in this document.

When starting the GUI, the user is prompted to enter connection data for the Analysis Server. After clicking "Connect", the user is either shown an error that the connection failed or the user is allowed to use the full functionality of the application. If the connection breaks for any reason during runtime, the user must be notified and shall again be prompted to reestablish a connection.

## Live Sensor Data in GUI (**6P**)
Live data of all sensors should be displayed somewhere in the GUI. The data should be updated every 10 or so seconds. It is sufficient to display the data in a table with 3 columns, namely location, metric and latest value.

## Line Chart (**7P**)
The user selects one sensor and enters a start and end time in a conventient way (date picker). Furthermore the user picks an interval (1 minute, 1 hour, 1 day or any other value) and executes the query. The retrieved data should be presented as a line chart. Pick your own library for displaying charts.

## Scatter Plot (**7P**)
The user selects two distinct sensors, a start and end time and an interval and then executes the query. The Client requests the two time series from the Analysis Server and displays the data as a scatter plot. Each point represents a point in time with the X coordinate having the value of the first sensor and the Y coordinate that of the second sensor.

# Assignment 2 - Clustering, Similarity Search, Decentralization
The second assignment consists of implementing two advanced data visualizations. Clustering is used to automatically find patterns in a huge data set with very little user input given. For example, the program can differentiate between work days and weekends solely based on the amount of CO2 present in a room. A simple algorithm for clustering is k-Means, for which a good amount of libraries exist. Similarity Search on the other hand takes a curve drawn by the user and finds intervals that resemble the curve as close as possible. When a window is opened for example, a sharp drop in temperature occurs, resulting in a detectable dent in the graph. Since Similarity Search is computationally intensive, the server should split it into separate tasks that are then delegated to one or more clients for completion.

The second assignment is comprised of these tasks, yielding 50 points in total:

## `cluster` Command (**10P**)
Educate yourself on how the k-Means algorithm works. In a two-dimensional scenario it takes a set of points described by their Carthesian coordinates and a number *k* telling how many clusters there should be. It returns *k* clusters, with each cluster having the following:

* A subset of the original input points (cluster members) that are part of this and only this cluster
* The center of the cluster, which is the arithmetic mean of its members and isn't necessarily contained in its member set
* The total error within the cluster

Now instead of working with two-dimensional points, one can also represent a data series as a point. One day with an interval of 1 hour results in a 24-dimensional point. The same mathematical rules that apply for two dimensions also work with more dimensions. For your convenience, there are already a range of libraries that implement the k-Means algorithm in a generic way.

The Client sends a request to the Analysis Server containing a `Sensor` instance and a start (inclusive) and end time (exclusive). The `intervalPoints` parameter sets the interval that the raw input data is sampled in. The resulting data series is then sanitized by interpolating missing data points. The method `DataSeries.interpolate()` does that job. The whole series is then sliced into consecutive sub series with the length `intervalClusters`. Series with one or more missing values are thrown out. Each series is normalized so that the lowest and highest value within a series are 0 and 1 respectively, using the method `DataSeries.normalize()`. For example, the CLI parameters `cluster HSi12 humidity 2019-09-01 2019-10-01 120 5 10` result in a total of `30 * 24 * 60 / 120 = 360` points with `120 / 5 = 24` dimensions each, being assigned to 10 clusters.

## Cluster Visualization in GUI (**10P**)
The user selects a sensor, a start and end time, intervals and enters a number of clusters. The user is presented with an overview of all clusters sorted by error, with each cluster being represented by its own line chart. The error and number of members for each cluster is also displayed. When selecting a cluster, a more detailed view with all cluster members, their timestamps and errors is shown. You can either present this information in a new tab or open another window. Each cluster member is displayed in a line chart. You can decide if there is a separate chart for each curve or if all curves are presented in one chart. If you choose the latter, the timestamp of a curve should be shown when hovering it and the thicker the curve, the lower the error.

## `sim` Command (**10P**)
The Client sends a request to the Analysis Server containing a metric, a start and end time, a minimum and maximum window size, **maxResultCount** and a dimensionless reference curve. The Analysis Server fetches all data within the given interval from all sensors with the given metric. The reference curve, which must contain at least 2 points, is normalized to values between 0 and 1. All input sensor data is sanitized by interpolating missing data points. The reference curve is scaled to different timespans ranging from the minimum window size to the maximum window size. Assuming that the reference curve has **r** points and the time series has **t** values, then there are **t - r + 1** sub series to be extracted from the time series, each with length **r**. Each sub series is normalized and compared to the reference curve using an euclidean distance function. All matched sub series are sorted descendingly by their error and the best **maxResultCount** matches are sent to the Client. Each match should contain the sensor it comes from, its data series and the calculated error.

There is plenty of room for freedom regarding your implementation of similarity search:

* How is data scaled to different window sizes? Do you scale the reference curve or the input data?
* Should the window size grow linearly or exponentially? Which one makes more sense?
* What is the interval you should fetch the input data with?
* What kind of data series should you send back to the Client? Interval? Normalized?

## Distribution of Similarity Search (**10P**)
Since similarity search involves many computations that can be parallelized, your task is to let the Analysis Server distribute work to connected clients. Adapt your protocol so that the Analysis Server keeps track of connected clients and each client can handle requests too. When the Analysis Server receives a similarity search request, it does some preparations and splits the workload into multiple, smaller jobs. The Analysis Server should delegate these jobs to connected clients efficiently, so that no client processes more than 5 jobs at once. As soon as all jobs are completed, a final response is computed and sent back to the requesting client. When a client processes a job, it should log the job parameters in the console. The server should also log which job is assigned to which client.

You should think of an efficient way to split the workload, so that it can be conventiently distributed no matter the number of connected clients:

* Create one job for each client by splitting the total window size range into multiple intervals?
* One job for each window size?
* One job per sensor?
* One job per extracted sub series?

What kind of information should the Analysis Server send to the clients? Only the request parameters or also sensor data? Which computations should the clients do and which ones the Analysis Server? What should happen if one client fails to do its job by disconnecting or throwing an error? Abort the overall computation, send the job to another client, ignore the error?

## Sketch Based Search and Visualization (**10P**)
The user selects a metric, a start and end time and draws a curve that is then used as a reference curve. The response can be visualized by giving a list of results, with each entry showing a line chart and its timestamp, location and error. Alternatively, you can implement the same approach as in "Cluster Visualization in GUI" with one line chart.

# Bonus Tasks
There is a wide range of additional features to implement such as improvements to the UI, advanced visualizations or performance optimizations. You are free to come up with your own ideas, but you should ask us for the number of points you can achieve for your idea before you hand in your submission. Please include a description of your bonus tasks in `SUBMISSION.md`. Here is a list of ideas with things you can implement as a bonus:

* *Writing cached data to disk* - The Analysis Server saves its cache content to a file in regular intervals and loads data from that file on startup.
* *History of recent queries* - The Client keeps a list of queries recently made and an option to load the parameters into the input fields and to repeat the query. More points if the history is stored in a file.
* *Non Maxima Suppression for Similarity Search* - Since many intervals in the results may overlap, find a way to only return one result per distinct feature. For example, remove a result if it overlaps with another result with lower error.
* *Clustering multiple sensors* - As of now, one feature descriptor contains one time series (e.g. hourly temperature in HSi12 for one day = 24 dimensions). Instead of selecting one sensor for clustering, the user can select one or more. This means that feature descriptor can contain multiple time series (e.g. hourly temperature and humidity in HSi12 for one day = 48 dimensions).
* *Unit Tests* - Write tests using JUnit that are automatically run when building the project. 1 Point per feature tested.
* *Protocol without serialization* - Instead of using serialization to convert between bytes and objects, find a more efficient way to encode and decode packets yourself.
* *Live data as line chart* - Instead of merely displaying live data in a table, a line chart should show data from the last 10 minutes or so. The chart should be updated every few seconds.
* *Line chart with multiple sensors* - The user can select multiple sensors to be displayed in one line chart. If two metrics are selected, the scale of one metric is displayed on the left side of the chart and the other metric on the right side.
* *Job overview in the GUI* - Add an additional tab to the GUI containing a list of similarity jobs that have been processed by this client. When a job is completed, its parameters (sensor, timespan, interval etc...) get appended to this list.

# Grading Scheme
There are a total of 100 points to be achieved, with each assignment counting 50 points. The points you get on an assignment including bonus points cannot exceed 60 points. The final grade depends on the overall number of points you achieve:
```java
public static Grade getGrade(float ass1, float ass2) {
  ass1 = Math.min(ass1, 60);
  ass2 = Math.min(ass2, 60);
  float total = ass1 + ass2;
  if (total >= 50 && ass1 >= 10 && ass2 >= 10) {
    if (total >= 86) {
      return Grade.SEHR_GUT; // 1
    }
    if (total >= 74) {
      return Grade.GUT; // 2
    }
    if (total >= 62) {
      return Grade.BEFRIEDIGEND; // 3
    }
    return Grade.GENUEGEND; // 4
  }
  return Grade.NICHT_GENUEGEND; // 5
}
```

The code above implies that you need to have at least 10 points on both assignments and at least 50 points in total to pass the course. If you fail to achieve at least 25 points (excluding deductions at the Abgabegespräch) on an assignment, you have the chance to deliver a second submission. The assignment will then be graded with `points < 25 ? points : 25 + (points - 25) / 2` points.

For both of the assignments there will be Abgabegespräche for each team shortly after the deadline, where you must present your work and explain your code. All team members must be present. We'll also have a quick look into your Git commit statistics. If we find out that you didn't participate sufficiently, you'll get individual point deductions. Plagiarism is not tolerated and leads to a negative grade for the whole team and other teams involved.
