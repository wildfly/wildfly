/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.deployment;

import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.wildfly.extension.undertow.ImportedClassELResolver;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.jsp.JspApplicationContext;
import jakarta.servlet.jsp.JspFactory;
import java.util.List;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Listener that sets up the {@link JspApplicationContext} with any wrapped EL expression factories and also
 * setting up any relevant {@link jakarta.el.ELResolver}s
 *
 * @author Stuart Douglas
 */
public class JspInitializationListener implements ServletContextListener {

    public static final String CONTEXT_KEY = "org.jboss.as.web.deployment.JspInitializationListener.wrappers";
    private static final String DISABLE_IMPORTED_CLASS_EL_RESOLVER_PROPERTY = "org.wildfly.extension.undertow.deployment.disableImportedClassELResolver";

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        // if the servlet version is 3.1 or higher, setup a ELResolver which allows usage of static fields java.lang.*
        final ServletContext servletContext = sce.getServletContext();
        final JspApplicationContext jspApplicationContext = JspFactory.getDefaultFactory().getJspApplicationContext(servletContext);
        boolean disableImportedClassELResolver = Boolean.parseBoolean(
                WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(DISABLE_IMPORTED_CLASS_EL_RESOLVER_PROPERTY));
        if (!disableImportedClassELResolver &&
                (servletContext.getEffectiveMajorVersion() > 3 ||
                (servletContext.getEffectiveMajorVersion() == 3 && servletContext.getEffectiveMinorVersion() >= 1))) {
            jspApplicationContext.addELResolver(new ImportedClassELResolver());
        }
        // setup a wrapped JspApplicationContext if there are any EL expression factory wrappers for this servlet context
        final List<ExpressionFactoryWrapper> expressionFactoryWrappers = (List<ExpressionFactoryWrapper>) sce.getServletContext().getAttribute(CONTEXT_KEY);
        if (expressionFactoryWrappers != null && !expressionFactoryWrappers.isEmpty()) {
            final JspApplicationContextWrapper jspApplicationContextWrapper = new JspApplicationContextWrapper(JspApplicationContextImpl.getInstance(servletContext), expressionFactoryWrappers, sce.getServletContext());
            sce.getServletContext().setAttribute(JspApplicationContextImpl.class.getName(), jspApplicationContextWrapper);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {

    }
}
