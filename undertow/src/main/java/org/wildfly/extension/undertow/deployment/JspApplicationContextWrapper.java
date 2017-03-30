/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.undertow.deployment;

import java.util.List;

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspContext;

import org.apache.jasper.el.ELContextImpl;
import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.jboss.as.web.common.ExpressionFactoryWrapper;

/**
 * @author pmuir
 */
public class JspApplicationContextWrapper extends JspApplicationContextImpl {

    private final JspApplicationContextImpl delegate;
    private final List<ExpressionFactoryWrapper> wrapperList;
    private final ServletContext servletContext;
    private volatile ExpressionFactory factory;

    protected JspApplicationContextWrapper(JspApplicationContextImpl delegate, List<ExpressionFactoryWrapper> wrapperList, ServletContext servletContext) {
        this.delegate = delegate;
        this.wrapperList = wrapperList;
        this.servletContext = servletContext;
    }

    @Override
    public void addELContextListener(ELContextListener listener) {
        delegate.addELContextListener(listener);
    }

    @Override
    public void addELResolver(ELResolver resolver) throws IllegalStateException {
        delegate.addELResolver(resolver);
    }

    @Override
    public ELContextImpl createELContext(JspContext arg0) {
        return delegate.createELContext(arg0);
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        if (factory == null) {
            synchronized (this) {
                if (factory == null) {
                    factory = delegate.getExpressionFactory();
                    for (ExpressionFactoryWrapper wrapper : wrapperList) {
                        factory = wrapper.wrap(factory, servletContext);
                    }
                }
            }
        }
        return factory;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
