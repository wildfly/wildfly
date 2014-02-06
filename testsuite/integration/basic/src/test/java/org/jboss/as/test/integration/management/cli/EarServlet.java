package org.jboss.as.test.integration.management.cli;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Stuart Douglas
 */
@WebServlet(urlPatterns = "/EarServlet")
public class EarServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream resource = getClass().getClassLoader().getResourceAsStream("jar-info.txt");
        try {
            byte[] data = new byte[100];
            int res;
            while ((res = resource.read(data)) > 0) {
                resp.getOutputStream().write(data, 0, res);
            }
        } finally {
            resource.close();
        }
    }
}
