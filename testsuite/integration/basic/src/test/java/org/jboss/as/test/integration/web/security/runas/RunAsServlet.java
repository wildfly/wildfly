package org.jboss.as.test.integration.web.security.runas;

import java.io.IOException;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
