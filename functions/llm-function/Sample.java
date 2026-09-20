import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.catalyst.advanced.CatalystAdvancedIOHandler;
import com.zc.component.connections.ZCConnections;
import com.zc.component.connections.beans.ZCConnectionResponse;

public class Sample implements CatalystAdvancedIOHandler {
    private static final Logger LOGGER = Logger.getLogger(Sample.class.getName());

    // Step 1: Get these two values from QuickML > LLM Serving > GLM-4.7-Flash in your Catalyst console
    private static final String LLM_URL = "https://api.catalyst.zoho.in/quickml/v1/project/56071000000013024/glm/chat";
    private static final String CATALYST_ORG = "60076411433";

    @Override
    public void runner(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");

        try {
            switch (request.getRequestURI()) {

                case "/ask": {
                    // Read the prompt sent from the frontend
                    String prompt = request.getReader().lines()
                            .collect(Collectors.joining());

                    // Step 2: Get auth token from Catalyst Connections
                    // Go to Cloud Scale > Connections > create a connection with Catalyst by Zoho
                    // Set scope to QuickML.deployment.READ
                    // Replace "YOUR_CONNECTION_LINK_NAME" with your connection's link name
                    ZCConnections connections = ZCConnections.getInstance();
                    ZCConnectionResponse credentials = connections.getConnectionCredentials("myconnection");
                    Map<String, String> authHeaders = credentials.getHeaders();

                    // Build the request payload for the LLM
                    String llmPayload = "{"
                        + "\"model\": \"crm-di-glm47b_30b_it\","
                        + "\"messages\": ["
                        + "  {\"role\": \"system\", \"content\": \"You are a helpful Java assistant. Answer concisely and directly.\"},"
                        + "  {\"role\": \"user\", \"content\": \"" + prompt + "\"}"
                        + "],"
                        + "\"max_tokens\": 800,"
                        + "\"stream\": false,"
                        + "\"chat_template_kwargs\": {\"enable_thinking\": false}"
                        + "}";

                    // Call the QuickML LLM API
                    HttpURLConnection conn = (HttpURLConnection) new URL(LLM_URL).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("CATALYST-ORG", CATALYST_ORG);

                    // Catalyst Connections injects the Authorization header automatically
                    for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }

                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(llmPayload.getBytes());
                    }

                    // Read the LLM response and send it back to the frontend
                    String llmResponse = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))
                        .lines().collect(Collectors.joining());

                    response.setStatus(200);
                    response.getWriter().write(llmResponse);
                    break;
                }

                case "/": {
                    response.setStatus(200);
                    response.getWriter().write("LLM Function is running");
                    break;
                }

                default: {
                    response.setStatus(404);
                    response.getWriter().write("{\"error\": \"Route not found\"}");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception in Sample", e);
            response.setStatus(500);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}