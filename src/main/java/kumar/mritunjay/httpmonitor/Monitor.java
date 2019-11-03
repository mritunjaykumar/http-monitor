package kumar.mritunjay.httpmonitor;

import kumar.mritunjay.httpmonitor.models.Alert;
import kumar.mritunjay.httpmonitor.models.LogRecord;
import kumar.mritunjay.httpmonitor.models.Stats;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;

/**
 * This class monitors http traffic. It alerts and print stats every given minutes.
 */
public class Monitor {
    private Stats stats;
    private Alert alert;

    /**
     * Creates monitor instance
     */
    @Autowired
    public Monitor(Stats stats, Alert alert) {
        this.stats = stats;
        this.alert = alert;
    }

    /**
     * Process the input file
     * @param inputFile
     */
    public void process(String inputFile) {
        boolean isHeader = true;
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
            String line;

            LogRecord logRecord = new LogRecord();

            while ((line = bufferedReader.readLine()) != null) {
                // very first line is header. This is hard assumption. I am not doing any comparison here.
                if(isHeader) {
                    isHeader = false;
                    continue;
                }
                logRecord.populateRecord(line);

                alert.process(logRecord); // Process alert
                stats.process(logRecord); // Process stats
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
