package kumar.mritunjay.httpmonitor.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LogRecord {
    // This is the total number of items in a record after splitting the line on ',' delimiter
    private static int NUMBER_OF_ITEMS_IN_RECORD = 7;
    private String remoteHost;
    private String rfc931;
    private String authUser;
    private long date;
    private String request;
    private String Status;
    private int bytes;

    private String section;

    public void populateRecord(String line) {
        if(line == null || line.length() == 0) return;

        String[] items = line.split(",");

        // After splitting the record, if total number of items in the list
        // is not what expected, then ignore this bad record.
        // TODO: I am assuming that ALL of the records in the file are valid (7 items long).
        if(items.length != NUMBER_OF_ITEMS_IN_RECORD) return;

        setRemoteHost(items[0]);
        setRfc931(items[1]);
        setAuthUser(items[2]);
        setDate(Long.parseLong(items[3]));
        setRequest(items[4]);
        setSection(request);
        setStatus(items[5]);
        setBytes(Integer.parseInt(items[6]));
    }

    private void setSection(String request) {
        String[] requestItems = request.trim().split("\\s+"); // Split request string on whitespaces
        String[] sectionArray = requestItems[1].trim().split("/"); // Split path to get the section
        if(sectionArray.length > 1) {
            section = "/" + sectionArray[1];
        }
        else {
            // I am assuming this is an invalid case. So, do nothing
        }
    }
}
