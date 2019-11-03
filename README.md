# http-monitor
This is a spring boot application.
## how to run
Using `Clone or download` button, first download (`Download ZIP`) the repo locally.
Unzip this project into some folder.
* Open the terminal window, and go to the project folder.
* Run `mvn clean install` to build the project. This will also run the test cases.

Now, from the project home folder in the terminal window, you can run the 
following command:
```
java -jar target/http-monitor-0.0.1-SNAPSHOT.jar sample_csv.txt
```
* `sample_csv.txt` is the file that contains all of the log data

* application.properties file:
    ```
    # Stats configs
    time.bucket.duration.in.seconds=10
    cron-config="*/1 * * * * *"
    
    # Alert configs
    alert.window.in.seconds=120
    alert.threshold=1000
    ```
    * `time.bucket.duration.in.seconds` is the time bucket for stats gathering
    * `cron-config` defines when to print the stats. Right now, it's printing
    every second.
    * `alert.window.in.seconds` specifies the time-window that is used while
    processing the alert logic. It's set to 2 minutes in this setting.
    * `alert.threshold` specifies the threshold value for alert.

    You can use `application.properties` file to configure this system.
## Solution Details
`HttpMonitorConfig` class is a config class which creates following beans:
* `Stats` - this bean keep track of hit-count stats for different sections.
* `Alert` - this bean keep checking on alert.
* `Monitor` - this is the container bean which contains both `Stats` and
`Alert` beans. This is the class which processes the input log file. It's 
`process` method processes the input file linearly and pass on each record
to `Stats` and `Alert` beans to process stats and alerts.

### Alert Class
* `alertWindowInSeconds` - this is used to calculate number of records
 in that time duration. 
* `alertThreshold` - this is used to check whether to trigger alert.
* `timestampQueue` - this queue contains timestamps to process the alert logic.
Also, this queue is sorted in ascending order based on timestamp.
* `process` - this is the method which is processing the record and performing all 
of the alert logic with help of some other important `private` methods.
#### Alert Algorithm
* Get the record to process
* Create the alert window - start date is `currentDate` and end date is 
`endDateForAlertWindow`
* Point to note that these records are NOT in sorted order, so older record may 
show up later while processing the file. So, `currentDate` is added to the
sorted queue `timestampQueue`
    * `addToSortedQueue` method has four sections:
        * if `timestampToAdd` is greater than or equal to the last item, then add 
        to the last.
        * if `timestampToAdd` is less than or equal to the first item, then add to
        the beginning of the sorted queue.
        * now, `timestampToAdd` need to be somewhere in the middle of the sorted list, 
        so find its position using method `getPositionToInsert`. Using Binary Search 
        algorithm to search for the position for the `timestampToAdd`. Linear search 
        can take O(n) operations. But with Binary Search we can get that position 
        in O(log n).
* Once the current record is added to the sorted queue, it's time to remove the stale
entries from it. So, loop through the sorted queue and remove all of the stale
entries by comparing timestamp with `endDateForAlertWindow`.
* Now, if size of the sorted queue is bigger than or equal to threshold, then alert.
Also turn on the alert flag `currentlyInAlert`, so that we don't generate duplicate 
alert.
* Similar logic for alert recovery.

### Stats Class
* `timeBucketDurationInSeconds` - this is used to create the time boundary to gather
the hit counts for different sections.
* `statsQueue` - this is the queue which contains the stats messages to print.
* `sectionHitCountMap` - Sections and their hit count for given time bucket
* `monitorStartTime` - This the beginning of the monitor. This should be the nearest 
previous 10s block. For example, if log file's first record timestamp is 14:23:37, 
then value for this field should be 14:23:30. This is to make sure, when we have 
10s bucket, first record is getting in its own "10s" bucket.
* while processing, `bucketStartTime` and `bucketEndTime` defines the time-bucket
boundary.
* `process` is the method which act on the log record to capture the stats

#### Stats algorithm
* get the record
* if current record's date is less than `bucketStartTime`, then skip the 
stale and late-arrived data. (Here, I am assuming that we are not processing 
late-arrivals). 
* if current record's date is greater than or equal to `bucketEndTime`, then 
we are done collecting data for the current date-bucket. Wrap up the current 
bucket and start the next one.
    * add gathered stats to the `statsQueue`.
    * clean up the `sectionHitCountMap` bucket and reset traffic volume and
    `biggestHitCountSection` for the new bucket.
    * create the new date-bucket to gather next round of stats
* while processing records, keep track of `biggestHitCountSection`.

#### Print Stats From The Queue at given interval
`@Scheduled(cron = "${cron-config}")` makes sure that it's running 
`printStatsFromTheQueue` method at the interval specified by the `cron-config`.
 
## Future Improvements
#### Scale and High Availability (HA)
Assuming we are processing log data from thousands of servers, I would pick
`Kafka` as data bus. This will help horizontally scale our data intake. We can
have agents running on those servers (hosts) which will `produce` data into a
`Kafka topic`. At the same time, on the consumer side, I would wrap my 
code into `Kafka consumer` and put that into `docker` container. This container 
can be easily deployed in `Kubernetes` cluster using `Helm charts`. 
We can run many replicas of the containerized code on `kubernetes pods` 
as stateless service. This will scale horizontally. While, we are scaling out on the 
consumer side, we also need to be aware of the fact that we have a hard limit on that. 
And that hard limit is partition count on the `Kafka topic`. We can't have 
more instances of consumers running than the partition count. Once number of 
consumers is equal to number of partitions, we can't scale further. To scale further,
we need add more partitions.

#### Security
Regarding security, we need to make sure, when we are consuming from kafka topic,
we are using some `secret` to talk to kafka in secure way.

#### Persistence
Regarding persisting stats data, I can think of writing this data into some
time-series database (InfluxDB for example) with some data-retention policy. I know
that stats data is already aggregated, but there could be some scenario where business
wants to persist aggregated data for some time. Regarding alert data, we might persist
this into some relational database if we want to keep this information for much
longer time. I chose relational in alert data case mainly because of low volume of
data which don't need much traffic. However, if there is need of alert data being
used by multiple teams for analytics or other things, we can route this data back
to `Kafka` into some different topic. From there, other teams can consume these data
and process them based on their needs.