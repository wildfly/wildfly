package org.jboss.as.test.integration.web.security.runas;

import java.io.IOException;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
@WebServlet(name = "RunAsServlet", urlPatterns = "/runAs")
@RunAs("peter")
public class RunAsServlet extends HttpServlet {

    @EJB
    private CurrentUserEjb currentUserEjb;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(currentUserEjb.getCurrentUser());
    }
}
