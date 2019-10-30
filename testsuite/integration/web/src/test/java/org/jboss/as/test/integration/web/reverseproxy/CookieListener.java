package org.jboss.as.test.integration.web.reverseproxy;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author Stuart Douglas
 */
@WebListener
public class CookieListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().getSessionCookieConfig().setPath("/");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
