package kumar.mritunjay.httpmonitor;

import kumar.mritunjay.httpmonitor.models.Alert;
import kumar.mritunjay.httpmonitor.models.LogRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class AlertTest {

    @Test
    public void process_trafficCountEqualToThresholdWithOldDataComingLater_should_trigger_alert() {
        Alert alert = new Alert(10,5);

        List<LogRecord> records = createTestLogRecordsInReverseOrder(1549573860L, 5);

        for(LogRecord rec : records) {
            alert.process(rec);
        }

        Assertions.assertTrue(alert.isCurrentlyInAlert(), "Alert didn't trigger");
    }

    @Test
    public void process_trafficCountEqualToThreshold_should_trigger_alert() {
        Alert alert = new Alert(10,5);

        List<LogRecord> testLogRecords = createTestLogRecords(1549573860L, 5);

        for(LogRecord rec : testLogRecords) {
            alert.process(rec);
        }

        Assertions.assertTrue(alert.isCurrentlyInAlert(), "Alert didn't trigger");
    }

    @Test
    public void process_trafficCountEqualToThresholdWithDataComingRandomly_should_trigger_alert() {
        Alert alert = new Alert(10,5);

        List<LogRecord> records = createTestLogRecordsWithUnorderedTimestamps(1549573860L);

        for(LogRecord rec : records) {
            alert.process(rec);
        }

        Assertions.assertTrue(alert.isCurrentlyInAlert(), "Alert didn't trigger");
    }

    @Test
    public void process_trafficCountLessThanThreshold_shouldNot_trigger_alert() {
        Alert alert = new Alert(10,5);

        List<LogRecord> testLogRecords = createTestLogRecords(1549573860L, 4);

        for(LogRecord rec : testLogRecords) {
            alert.process(rec);
        }

        Assertions.assertFalse(alert.isCurrentlyInAlert(), "Alert triggered");
    }

    @Test
    public void process_trafficCountMoreThanThreshold_should_trigger_alert() {
        Alert alert = new Alert(10,5);

        List<LogRecord> testLogRecords = createTestLogRecords(1549573860L, 6);

        for(LogRecord rec : testLogRecords) {
            alert.process(rec);
        }

        Assertions.assertTrue(alert.isCurrentlyInAlert(), "Alert didn't trigger");
    }

    @Test
    public void process_systemInAlertAndTrafficCountLessThanThreshold_should_recover_alert() {
        Alert alert = new Alert(5,5);

        List<LogRecord> recForAlert = createTestLogRecords(1549573860L, 6);
        for(LogRecord rec : recForAlert) {
            alert.process(rec);
        }

        // Here, it should trigger alert
        Assertions.assertTrue(alert.isCurrentlyInAlert(), "Alert didn't trigger");

        // Add a couple of records AFTER a gap of a few seconds to make sure we have less records than threshold
        // in the alert window. This should recover the alert
        for(LogRecord rec : createTestLogRecords(1549573870L, 4)) {
            alert.process(rec);
        }

        Assertions.assertFalse(alert.isCurrentlyInAlert(), "Alert triggered");
    }

    private List<LogRecord> createTestLogRecordsWithUnorderedTimestamps(long startDate) {
        List<LogRecord> records = new ArrayList<>();

        for(int i : new int[] {3, 1, 0, 2, 5, 4}) {
            LogRecord rec = new LogRecord();
            long date = startDate + (long) i;
            String line = String.format("\"10.0.0.2\",\"-\",\"apache\",%s,\"GET /api/help HTTP/1.0\",200,1234",
                    date);
            rec.populateRecord(line);
            records.add(rec);
        }

        return records;
    }

    private List<LogRecord> createTestLogRecordsInReverseOrder(long startDate, int numberOfRecordsToCreate) {
        List<LogRecord> records = new ArrayList<>();

        int count = numberOfRecordsToCreate;
        while(count >= 0) {
            LogRecord rec = new LogRecord();
            long date = startDate + (long) count--;
            String line = String.format("\"10.0.0.2\",\"-\",\"apache\",%s,\"GET /api/help HTTP/1.0\",200,1234",
                    date);
            rec.populateRecord(line);
            records.add(rec);
        }

        return records;
    }

    private List<LogRecord> createTestLogRecords(long startDate, int numberOfRecordsToCreate) {
        List<LogRecord> records = new ArrayList<>();

        int count = 0;
        while(count < numberOfRecordsToCreate) {
            LogRecord rec = new LogRecord();
            long date = startDate + (long) count++;
            String line = String.format("\"10.0.0.2\",\"-\",\"apache\",%s,\"GET /api/help HTTP/1.0\",200,1234",
                    date);
            rec.populateRecord(line);
            records.add(rec);
        }

        return records;
    }
}
