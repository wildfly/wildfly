package org.jboss.as.capedwarf.api;

import org.jboss.as.capedwarf.services.ServletExecutor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet executor listener.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ServletExecutorListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        final String appId = (String) context.getAttribute("org.jboss.capedwarf.appId");
        ServletExecutor.registerContext(appId, context);
    }

    public void contextDestroyed(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        final String appId = (String) context.getAttribute("org.jboss.capedwarf.appId");
        ServletExecutor.unregisterContext(appId);
    }
}
