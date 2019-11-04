package kumar.mritunjay.httpmonitor.models;

import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Stats {
    private static String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    // A bucket to gather the stats
    private int timeBucketDurationInSeconds;

    private int trafficVolume;              // Traffic volume for given bucket
    private String biggestHitCountSection;  // section in the bucket which has biggest hit count
    private LinkedList<String> statsQueue;  // This keeps the stats messages to print

    // Sections and their hit count for given time bucket
    private Map<String, Integer> sectionHitCountMap;

    // This the beginning of the monitor. This should be the nearest previous 10s block.
    // For example, if log file's first record timestamp is 14:23:37, then value for this
    // field should be 14:23:30. This is to make sure, when we have 10s bucket, first record is
    // getting in its own "10s" bucket.
    private long monitorStartTime;

    private long bucketStartTime;
    private long bucketEndTime;


    boolean processedFirstLogData = false;

    public Stats(int timeBucketDurationInSeconds) {
        this.timeBucketDurationInSeconds = timeBucketDurationInSeconds;
        this.trafficVolume = 0;
        this.bucketStartTime = 0L;
        this.bucketEndTime = 0L;
        this.biggestHitCountSection = "";
        this.sectionHitCountMap = new HashMap<>();
        this.statsQueue = new LinkedList<>();
    }

    @Scheduled(cron = "${cron-config}")
    public void printStatsFromTheQueue() {
        if(statsQueue.size() == 0) return;

        // Remove and print the oldest item in the queue
        System.out.println(statsQueue.removeFirst());
    }

    /**
     * Process log record to capture the stats
     * @param logRecord
     */
    public void process(LogRecord logRecord) {
        long date = logRecord.getDate();
        if(!processedFirstLogData) {
            monitorStartTime = calculateMonitorStartTime(date);

            bucketStartTime = monitorStartTime;
            bucketEndTime = getBucketEndTime(bucketStartTime);
            processedFirstLogData = true;
        }

        // Skip stale and late-arrived data
        // somewhere we need to draw the line. When to stop looking for data for old bucket.
        // Here I am assuming that we are not waiting for any old data.
        if(date < bucketStartTime) return;

        // We are done collecting data for the current date-bucket
        // wrap up the current bucket and start the next one
        if(date >= bucketEndTime) {
            // add stats to the stats queue
            addStatsToTheStatsQueue();

            // clean up the bucket and reset the traffic volume for the new bucket
            sectionHitCountMap.clear();
            trafficVolume = 0;
            biggestHitCountSection = "";

            bucketStartTime = bucketEndTime;
            bucketEndTime = getBucketEndTime(bucketStartTime);
        }

        String section = logRecord.getSection();
        trafficVolume++;

        Integer hitCount = sectionHitCountMap.get(section);
        if(hitCount == null) {
            sectionHitCountMap.put(section, 1); // Initialize hit count for this section
        }
        else {
            sectionHitCountMap.put(section, ++hitCount); // Increment hit-count for the section
        }

        // set or update biggestHitCountSection
        if(biggestHitCountSection.equals("") ||
                (sectionHitCountMap.get(biggestHitCountSection) < sectionHitCountMap.get(section))) {
            biggestHitCountSection = section;
        }
    }

    private void addStatsToTheStatsQueue() {
        StringBuilder sb = new StringBuilder();
        sb.append("Stats for start time: [" + convertEpochToDateString(bucketStartTime) +
                "] and end time: [" + convertEpochToDateString(bucketEndTime) + "] \n");
        sb.append("Total traffic volume or message count is [" + trafficVolume + "]\n");
        sb.append("Section with most hits is [" +
                biggestHitCountSection +
                "] with hit count of [" +
                sectionHitCountMap.get(biggestHitCountSection) + "]\n");
        sb.append("Following is the complete list of the sections with their hit count: \n");

        for(String section : sectionHitCountMap.keySet()) {
            sb.append("\tsection [" + section + "] : hit-count [" + sectionHitCountMap.get(section) + "]\n");
        }

        sb.append("\n");
        statsQueue.addLast(sb.toString());
    }

    private long getBucketEndTime(long bucketStartTime) {
        Instant temp = Instant.ofEpochSecond(bucketStartTime);
        temp = temp.plusSeconds(timeBucketDurationInSeconds);

        return temp.getEpochSecond();
    }

    private long calculateMonitorStartTime(long date) {
        long offset = date % ((long) timeBucketDurationInSeconds);
        return date - offset;
    }

    // TODO: This is a duplicate code. As an improvement, I can put this in some common util library.
    // But, leaving this dup code as it is for now.
    private String convertEpochToDateString(long epochTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);

        return simpleDateFormat.format(new Date(epochTime * 1000)); // Convert Date's parameter from sec to milli sec.
    }
}
