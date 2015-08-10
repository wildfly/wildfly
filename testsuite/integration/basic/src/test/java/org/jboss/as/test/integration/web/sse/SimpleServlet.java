package org.jboss.as.test.integration.web.sse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Stuart Douglas
 */
@WebServlet(urlPatterns = "/simple")
public class SimpleServlet extends HttpServlet {

    public static final String SIMPLE_SERVLET = "simple servlet";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(SIMPLE_SERVLET);
    }
}
