package org.jboss.as.web.host;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContext;

import org.jboss.as.server.deployment.AttachmentKey;

public interface ContextActivator {

    AttachmentKey<ContextActivator> ATTACHMENT_KEY = AttachmentKey.create(ContextActivator.class);

    /**
     * Start the web context asynchronously.
     * <p/>
     * This would happen during OSGi webapp deployment.
     * <p/>
     * No DUP can assume that all dependencies are available to make a blocking call
     * instead it should call this method.
     */
    void startAsync();

    /**
     * Start the web context synchronously.
     * <p/>
     * This would happen when the OSGi webapp gets explicitly started.
     */
    boolean start(long timeout, TimeUnit unit) throws TimeoutException;

    /**
     * Stop the web context synchronously.
     * <p/>
     * This would happen when the OSGi webapp gets explicitly stops.
     */
    boolean stop(long timeout, TimeUnit unit);

    /**
     * Return the servlet context
     *
     * @return The servlet context
     */
    ServletContext getServletContext();

}