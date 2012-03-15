package org.jboss.as.test.integration.weld.ejb.requestScope;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
@WebServlet(urlPatterns = "/*")
public class RequestServlet extends HttpServlet{

    @EJB(lookup = "ejb:/ejb/RemoteEjb!org.jboss.as.test.integration.weld.ejb.requestScope.RemoteInterface")
    private RemoteInterface remoteInterface;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(remoteInterface.getMessage());
        resp.getWriter().close();
    }
}
