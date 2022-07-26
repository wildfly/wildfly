package org.jboss.as.test.integration.weld.ejb.requestscope;

import java.io.IOException;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
@WebServlet(urlPatterns = "/*")
public class RequestServlet extends HttpServlet{

    @EJB(lookup = "ejb:/ejb/RemoteEjb!org.jboss.as.test.integration.weld.ejb.requestscope.RemoteInterface")
    private RemoteInterface remoteInterface;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(remoteInterface.getMessage());
        resp.getWriter().close();
    }
}
