/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.util;

import java.io.File;
import jakarta.servlet.Servlet;

import org.jboss.as.web.host.ServletBuilder;
import org.jboss.as.web.host.WebDeploymentController;
import org.jboss.as.web.host.WebDeploymentBuilder;
import org.jboss.as.web.host.WebHost;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.msc.service.StartException;

/**
 * WebAppController allows for automatically starting/stopping a webapp (servlet) depending on the actual need. This is useful
 * for WS deployments needing a given utility servlet to be up (for instance the port component link servlet)
 *
 * @author alessio.soldano@jboss.com
 * @since 02-Dec-2011
 */
public class WebAppController {

    private WebHost host;
    private String contextRoot;
    private String urlPattern;
    private String serverTempDir;
    private String servletClass;
    private ClassLoader classloader;
    private volatile WebDeploymentController ctx;
    private int count = 0;

    public WebAppController(WebHost host, String servletClass, ClassLoader classloader, String contextRoot, String urlPattern,
            String serverTempDir) {
        this.host = host;
        this.contextRoot = contextRoot;
        this.urlPattern = urlPattern;
        this.serverTempDir = serverTempDir;
        this.classloader = classloader;
        this.servletClass = servletClass;
    }

    public synchronized int incrementUsers() throws StartException {
        if (count == 0) {
            try {
                ctx = startWebApp(host);
            } catch (Exception e) {
                throw new StartException(e);
            }
        }
        return count++;
    }

    public synchronized int decrementUsers() {
        if (count == 0) {
            throw new IllegalStateException();
        }
        count--;
        if (count == 0) {
            try {
                stopWebApp(ctx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return count;
    }

    private WebDeploymentController startWebApp(WebHost host) throws Exception {
        WebDeploymentBuilder builder = new WebDeploymentBuilder();
        WebDeploymentController deployment;
        try {
            builder.setContextRoot(contextRoot);
            File docBase = new File(serverTempDir, contextRoot);
            if (!docBase.exists()) {
                docBase.mkdirs();
            }
            builder.setDocumentRoot(docBase);
            builder.setClassLoader(classloader);

            final int j = servletClass.indexOf(".");
            final String servletName = j < 0 ? servletClass : servletClass.substring(j + 1);
            final Class<?> clazz = classloader.loadClass(servletClass);
            ServletBuilder servlet = new ServletBuilder();
            servlet.setServletName(servletName);
            servlet.setServlet((Servlet) clazz.newInstance());
            servlet.setServletClass(clazz);
            servlet.addUrlMapping(urlPattern);
            builder.addServlet(servlet);

            deployment = host.addWebDeployment(builder);
            deployment.create();

        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.createContextPhaseFailed(e);
        }
        try {
            deployment.start();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.startContextPhaseFailed(e);
        }
        return deployment;
    }

    private void stopWebApp(WebDeploymentController context) throws Exception {
        try {
            context.stop();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.stopContextPhaseFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.destroyContextPhaseFailed(e);
        }
    }
}
