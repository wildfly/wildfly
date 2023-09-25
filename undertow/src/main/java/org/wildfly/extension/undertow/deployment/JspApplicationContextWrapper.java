/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.deployment;

import java.util.List;

import jakarta.el.ELContextListener;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.JspContext;

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
        // Before providing any ELContext, ensure we allow any ExpressionFactoryWrappers to execute
        getExpressionFactory();

        return delegate.createELContext(arg0);
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        if (factory == null) {
            synchronized (this) {
                if (factory == null) {
                    ExpressionFactory tmpfactory = delegate.getExpressionFactory();
                    for (ExpressionFactoryWrapper wrapper : wrapperList) {
                        tmpfactory = wrapper.wrap(tmpfactory, servletContext);
                    }
                    factory = tmpfactory;
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
