package kumar.mritunjay.httpmonitor.config;

import kumar.mritunjay.httpmonitor.Monitor;
import kumar.mritunjay.httpmonitor.models.Alert;
import kumar.mritunjay.httpmonitor.models.Stats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class HttpMonitorConfig {
    @Value("${time.bucket.duration.in.seconds}")
    private int timeBucketDurationInSeconds; // A bucket to gather the stats

    @Value("${alert.window.in.seconds}")
    int alertWindowInSeconds;

    @Value("${alert.threshold}")
    int alertThreshold;

    @Bean
    public Stats stats() {
        return new Stats(timeBucketDurationInSeconds);
    }

    @Bean
    public Alert alert() {
        return new Alert(alertWindowInSeconds, alertThreshold);
    }

    @Bean
    @Autowired
    public Monitor monitor(Stats stats, Alert alert) {
        return new Monitor(stats, alert);
    }
}
