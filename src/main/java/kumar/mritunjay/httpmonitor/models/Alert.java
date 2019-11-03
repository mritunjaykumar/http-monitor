package kumar.mritunjay.httpmonitor.models;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;

public class Alert {
    private static String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    private boolean currentlyInAlert; // Alert flag

    private int alertThreshold;

    // Time window value (in seconds) to look back for total traffic
    long alertWindowInSeconds;

    // This queue contains timestamps to process the alert logic.
    // Also, this queue is sorted in ascending order based on timestamp
    LinkedList<Long> timestampQueue;

    public Alert(int alertWindowInSeconds, int alertThreshold) {
        this.currentlyInAlert = false;
        this.alertThreshold = alertThreshold;
        this.alertWindowInSeconds = (long) (alertWindowInSeconds);
        this.timestampQueue = new LinkedList<>(); // Initialize queue
    }

    public boolean isCurrentlyInAlert() {
        return currentlyInAlert;
    }

    /**
     * This method process the log record and check whether system is in alert or not.
     * @param logRecord
     */
    public void process(LogRecord logRecord) {
        if(logRecord == null) return;

        Instant currentDate = Instant.ofEpochSecond(logRecord.getDate());
        Instant endDateForAlertWindow = currentDate.minusSeconds(alertWindowInSeconds);
        addToSortedQueue(currentDate.getEpochSecond());

        // Remove all of the timestamps from the queue that are older than the endDateForAlertWindow
        while(timestampQueue.peekFirst() < endDateForAlertWindow.getEpochSecond()) {
            timestampQueue.removeFirst();
        }

        // breached threshold AND currently not in alert; so alert now
        if((timestampQueue.size() >= alertThreshold) && !currentlyInAlert) {
            currentlyInAlert = true;
            System.out.println("******* High traffic generated an alert - hits = [" +
                    timestampQueue.size() +
                    "], triggered at [" +
                    convertEpochToDateString(timestampQueue.peekLast()) +
                    "] *******");
        }
        // record count goes less than threshold AND still in alert; so recover from alert
        else if((timestampQueue.size() < alertThreshold) && currentlyInAlert) {
            currentlyInAlert = false;
            System.out.println("******* Alert recovered - hits = [" +
                    timestampQueue.size() +
                    "], recovered at [" +
                    convertEpochToDateString(timestampQueue.peekLast()) +
                    "] *******");
        }
    }

    /**
     * Add 'timestampToAdd' to the sorted timestampQueue
     * @param timestampToAdd
     */
    private void addToSortedQueue(long timestampToAdd) {
        int size = timestampQueue.size();

        // timestampQueue is empty, so just add the timestampToAdd
        if(size == 0) {
            timestampQueue.add(timestampToAdd);
            return;
        }

        // timestampToAdd is bigger than or equal to the last element, so append it
        if(timestampToAdd >= timestampQueue.get(size-1)) {
            timestampQueue.addLast(timestampToAdd);
            return;
        }

        // timestampToAdd is smaller than or equal to the first element, so prepend it
        if(timestampToAdd <= timestampQueue.get(0)) {
            timestampQueue.addFirst(timestampToAdd);
            return;
        }

        // timestampToAdd need to be somewhere in the middle of the sorted list, so find its position
        int insertAtIndex = getPositionToInsert(timestampToAdd, 0, timestampQueue.size()-1);
        timestampQueue.add(insertAtIndex, timestampToAdd);
    }

    /**
     * Using Binary Search algorithm to search for the position for the timestampToAdd.
     * Linear search can take O(n) operations. But with Binary Search we can get that position in O(log n).
     * @param timestampToAdd
     * @param startIndex
     * @param endIndex
     * @return
     */
    private int getPositionToInsert(long timestampToAdd, int startIndex, int endIndex) {
        if(startIndex >= endIndex) {
            if(timestampQueue.get(endIndex) >= timestampToAdd) {
                return endIndex;
            }
            else {
                return endIndex+1;
            }
        }

        int mid = (startIndex + endIndex)/2;
        if(timestampQueue.get(mid) < timestampToAdd) {
            return getPositionToInsert(timestampToAdd, mid+1, endIndex);
        }
        else if(timestampQueue.get(mid) == timestampToAdd) {
            return mid;
        }
        else {
            return getPositionToInsert(timestampToAdd, startIndex, mid-1);
        }
    }

    /**
     * Convert epoch time to formatted date
     * @param epochTime
     * @return
     */
    private String convertEpochToDateString(long epochTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);

        return simpleDateFormat.format(new Date(epochTime * 1000)); // Convert Date's parameter from sec to milli sec.
    }
}
