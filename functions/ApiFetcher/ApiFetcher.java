import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.catalyst.Context;
import com.catalyst.job.CatalystJobHandler;
import com.catalyst.job.JOB_STATUS;
import com.catalyst.job.JobRequest;

public class ApiFetcher implements CatalystJobHandler {
    private static final Logger LOGGER = Logger.getLogger(ApiFetcher.class.getName());

    @Override
    public JOB_STATUS handleJobExecute(JobRequest request, Context context) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
            new URL("https://jsonplaceholder.typicode.com/todos/1").openConnection();
        conn.setRequestMethod("GET");

        String response = new BufferedReader(
            new InputStreamReader(conn.getInputStream()))
            .lines()
            .reduce("", String::concat);

        LOGGER.log(Level.INFO, "API Response: " + response);
        return JOB_STATUS.SUCCESS;
    }
}
