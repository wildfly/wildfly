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
package org.jboss.as.weld.webtier.jsp;

import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.jboss.as.weld.util.Reflections;
import org.jboss.weld.servlet.api.helpers.AbstractServletListener;

import javax.el.ELContextListener;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletRequestEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

/**
 * The Web Beans JSP initialization listener
 *
 *
 * @author Pete Muir
 * @author Stuart Douglas
 *
 */
public class JspInitializationListener extends AbstractServletListener {

    private volatile boolean installed = false;

    private static class WeldJspApplicationContextImpl extends ForwardingJspApplicationContextImpl {
        private final JspApplicationContextImpl delegate;
        private final ExpressionFactory expressionFactory;

        public WeldJspApplicationContextImpl(JspApplicationContextImpl delegate, ExpressionFactory expressionFactory) {
            this.delegate = delegate;
            this.expressionFactory = expressionFactory;
        }

        @Override
        protected JspApplicationContextImpl delegate() {
            return delegate;
        }

        @Override
        public ExpressionFactory getExpressionFactory() {
            return expressionFactory;
        }

    }

    @Inject
    private BeanManager beanManager;

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        if (!installed && beanManager!= null && JspFactory.getDefaultFactory() != null) {
            synchronized (this) {
                if (!installed) {
                    installed = true;
                    // get JspApplicationContext.
                    JspApplicationContext jspAppContext = JspFactory.getDefaultFactory().getJspApplicationContext(
                            sre.getServletContext());

                    // register compositeELResolver with JSP
                    jspAppContext.addELResolver(beanManager.getELResolver());

                    jspAppContext.addELContextListener(Reflections.<ELContextListener> newInstance(
                            "org.jboss.weld.el.WeldELContextListener", getClass().getClassLoader()));

                    // Hack into JBoss Web/Catalina to replace the ExpressionFactory
                    JspApplicationContextImpl wrappedJspApplicationContextImpl = new WeldJspApplicationContextImpl(
                            JspApplicationContextImpl.getInstance(sre.getServletContext()), beanManager
                                    .wrapExpressionFactory(jspAppContext.getExpressionFactory()));
                    sre.getServletContext().setAttribute(JspApplicationContextImpl.class.getName(),
                            wrappedJspApplicationContextImpl);
                }
            }
        }
        // otherwise something went wrong starting Weld, so don't register with JSP
    }

}