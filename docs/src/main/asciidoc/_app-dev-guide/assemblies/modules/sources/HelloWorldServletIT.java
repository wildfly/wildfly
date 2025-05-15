package org.jboss.as.quickstarts.helloworld;

import org.junit.Test;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static org.junit.Assert.assertEquals;

public class HelloWorldServletIT {

    private static final String DEFAULT_SERVER_HOST = "http://localhost:8080/helloworld";                //<1>

    @Test
    public void testHTTPEndpointIsAvailable() throws IOException, InterruptedException, URISyntaxException {
        String serverHost = System.getProperty("server.host");
        if (serverHost == null) {
            serverHost = DEFAULT_SERVER_HOST;
        }
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(serverHost+"/HelloWorld"))
                .GET()
                .build();                                                                                 //<2>
        final HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofMinutes(1))
                .build();                                                                                 //<3>
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); //<4>
        assertEquals(200, response.statusCode());                                                         //<5>
    }
}
