package org.jboss.as.web.host;

import javax.servlet.ServletContext;

import org.jboss.as.server.deployment.AttachmentKey;

public interface ContextActivator {

    AttachmentKey<ContextActivator> ATTACHMENT_KEY = AttachmentKey.create(ContextActivator.class);

    /**
     * Start the web context synchronously.
     * <p/>
     * This would happen when the OSGi webapp gets explicitly started.
     */
    boolean startContext();

    /**
     * Stop the web context synchronously.
     * <p/>
     * This would happen when the OSGi webapp gets explicitly stops.
     */
    boolean stopContext();

    /**
     * Return the servlet context
     *
     * @return The servlet context
     */
    ServletContext getServletContext();

}