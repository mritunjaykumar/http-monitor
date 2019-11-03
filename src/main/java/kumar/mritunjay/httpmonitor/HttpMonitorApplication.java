package kumar.mritunjay.httpmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class HttpMonitorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HttpMonitorApplication.class, args);

        if(args.length == 0) {
            System.out.println("Error: Missing input file.\n\n");
            System.out.println("Command to run -> java -jar target/http-monitor-0.0.1-SNAPSHOT.jar sample_csv.txt");
            return;
        }

        String input = args[0];

        // Get the monitor bean
        Monitor monitor = context.getBean(Monitor.class);

        // Process the input file
        monitor.process(input);
    }
}
