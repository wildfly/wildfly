import com.sun.net.httpserver.HttpServer;
import junit.framework.TestCase;
import org.jboss.as.domain.http.server.ConsoleHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.Executors;

/**
 * @author Heiko Braun
 * @date 3/14/11
 */
public class ConsoleHandlerTest extends TestCase {

    static final String[] failingNames= new String[] {
            "console", "xyz", "console/subdirectory", "console/subdirectory/"
    };

    static final String[] successfulNames = new String[] {
            "console/app/app.nocache.js", // within console.jar
            "console/index.html", "console/subdirectory/subresource.html" // within test/resources

    };

    private HttpServer server;

    @Override
    protected void setUp() throws Exception {
        startServer();
    }

    @Override
    protected void tearDown() throws Exception {
        stopServer();
    }

    public void testResourceLoading() throws Exception {

        // 404
        for(String resource : failingNames) {
            boolean isNotFound = false;
            try {
                GET(resource);
            } catch (Http404 notFound) {
                isNotFound = true;
            }

            assertTrue("Should respond 404 on "+ resource, isNotFound);
        }

        // 200
        try {
            for(String resource : successfulNames) {
                String res = GET(resource);
                System.out.println("> " +res);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to retrieve resource:" + e.getMessage());
        }

    }

    public void testContentTypes() throws Exception {
        HttpURLConnection uc = (HttpURLConnection) new URL("http://localhost:8080/console/test.css").openConnection();
        if(uc.getResponseCode()==404) fail("Not found!");

        assertEquals("Wrong content type", "text/css", uc.getContentType());
    }

    private void startServer() throws IOException {
        InetSocketAddress addr = new InetSocketAddress("localhost",8080);
        server = HttpServer.create(addr, 0);

        server.createContext(ConsoleHandler.CONTEXT, new ConsoleHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        server.start();
        System.out.println("Server is listening on " +addr);
    }

    private void stopServer() {
        server.stop(0);
        System.out.println("Server stopped");
    }

    private static String GET(String resource) throws Exception, Http404 {

        HttpURLConnection uc = (HttpURLConnection) new URL("http://localhost:8080/"+resource).openConnection();
        if(uc.getResponseCode()==404) throw new Http404();

        BufferedReader in = new BufferedReader( new InputStreamReader(uc.getInputStream()));
        String inputLine;

        StringBuffer sb = new StringBuffer();
        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine);

        in.close();
        return sb.toString();
    }

    static class Http404 extends Exception {
    }
}
