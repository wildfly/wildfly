import com.sun.net.httpserver.HttpServer;
import junit.framework.TestCase;
import org.jboss.as.domain.http.server.ConsoleHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executors;

/**
 * @author Heiko Braun
 * @date 3/14/11
 */
public class ConsoleHandlerTest extends TestCase {

    static final String[] failingNames= new String[] {
        "console", "xyz"
    };

    static final String[] successfulNames = new String[] {
        "console/index.html", "console/subdirectory/subresource.html"

    };

    public void testHandlerRegistration() throws Exception {

        startServer();

        // 404
        boolean hasFailed = false;
        try {
            for(String resource : failingNames) {
                String response = GET(resource);
            }
        } catch (Exception e) {
            hasFailed = true;
        }
        assertTrue(hasFailed);


        // 200
        try {
            for(String resource : successfulNames) {
                String response = GET(resource);
                System.out.println("> "+response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to retrieve resource:" + e.getMessage());
        }

    }

    private void startServer() throws IOException {
        InetSocketAddress addr = new InetSocketAddress("localhost",8080);
        HttpServer server = HttpServer.create(addr, 0);

        server.createContext(ConsoleHandler.CONTEXT, new ConsoleHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        server.start();
        System.out.println("Server is listening on " +addr);
    }


    private static String GET(String resource) throws Exception {

        URLConnection uc = new URL("http://localhost:8080/"+resource).openConnection();
        BufferedReader in = new BufferedReader( new InputStreamReader(uc.getInputStream()));
        String inputLine;

        StringBuffer sb = new StringBuffer();
        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine);

        in.close();
        return sb.toString();
    }
}
