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
@WebServlet(name = "RunAsInitServlet", urlPatterns = "/runAsInit", loadOnStartup = 100)
@RunAs("anil")
public class RunAsInitServlet extends HttpServlet {

    private volatile String initName;

    @EJB
    private CurrentUserEjb currentUserEjb;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(initName);
    }

    @Override
    public void init() throws ServletException {
        initName = currentUserEjb.getCurrentUser();
    }
}
