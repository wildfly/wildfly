package org.jboss.as.test.integration.web.security.runas;

import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author Stuart Douglas
 */
@WebServlet(name = "RunAsInitServlet", urlPatterns = "/runAsInit", loadOnStartup = 100)
@RunAs("Admin")
public class RunAsInitServlet extends HttpServlet {

    private String message = "defaultMsg";

    @EJB
    private CurrentUserEjb currentUserEjb;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(message);
    }

    @Override
    public void init() throws ServletException {
        // call helloAdmin() protected by @RolesAllowed("Admin")
        message = currentUserEjb.helloAdmin();
    }
}
