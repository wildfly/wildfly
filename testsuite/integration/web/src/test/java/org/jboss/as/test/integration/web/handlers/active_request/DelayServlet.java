package org.jboss.as.test.integration.web.handlers.active_request;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/test")
public class DelayServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

}
