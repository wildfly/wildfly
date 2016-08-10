/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.undertow.deployment;

import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.wildfly.extension.undertow.ImportedClassELResolver;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;
import java.util.List;

/**
 * Listener that sets up the {@link JspApplicationContext} with any wrapped EL expression factories and also
 * setting up any relevant {@link javax.el.ELResolver}s
 *
 * @author Stuart Douglas
 */
public class JspInitializationListener implements ServletContextListener {

    public static final String CONTEXT_KEY = "org.jboss.as.web.deployment.JspInitializationListener.wrappers";

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        // if the servlet version is 3.1 or higher, setup a ELResolver which allows usage of static fields java.lang.*
        final ServletContext servletContext = sce.getServletContext();
        final JspApplicationContext jspApplicationContext = JspFactory.getDefaultFactory().getJspApplicationContext(servletContext);
        if (servletContext.getEffectiveMajorVersion() >= 3 && servletContext.getEffectiveMinorVersion() >= 1) {
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
