package org.jboss.as.capedwarf.api;

import org.jboss.as.capedwarf.services.ServletExecutor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.Method;

/**
 * Servlet executor listener.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ServletExecutorListener implements ServletContextListener {
    private static final String PRODUCER_CLASS = "org.jboss.capedwarf.common.jms.ServletExecutorProducer";

    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        final String appId = (String) context.getAttribute("org.jboss.capedwarf.appId");
        ServletExecutor.registerContext(appId, context);
        // start producer
        checkProducer("start");
    }

    public void contextDestroyed(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        final String appId = (String) context.getAttribute("org.jboss.capedwarf.appId");
        ServletExecutor.unregisterContext(appId);
        // stop producer
        checkProducer("stop");
    }

    // Using reflection -- as we don't know how we're adding CD module; bundled or ref-ed as module
    private void checkProducer(final String state) {
        try {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader(); // app CL?
            final Class<?> clazz = cl.loadClass(PRODUCER_CLASS);
            final Method instance = clazz.getDeclaredMethod("getInstance");
            final Method method = clazz.getDeclaredMethod(state);
            final Object target = instance.invoke(null);
            method.invoke(target);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
