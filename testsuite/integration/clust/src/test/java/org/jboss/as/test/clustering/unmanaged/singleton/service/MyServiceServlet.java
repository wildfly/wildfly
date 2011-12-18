package org.jboss.as.test.clustering.unmanaged.singleton.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.server.CurrentServiceContainer;

@WebServlet(urlPatterns = { "/service" })
public class MyServiceServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String node = (String) CurrentServiceContainer.getServiceContainer().getService(MyService.SERVICE_NAME).getValue();
        resp.setHeader("node", node);
        resp.getWriter().write("Success");
    }
}
