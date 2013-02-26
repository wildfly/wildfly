/*
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
package org.jboss.as.web.deployment;

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspContext;

import org.apache.jasper.el.ELContextImpl;
import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.jboss.as.web.common.ExpressionFactoryWrapper;

import java.util.List;

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
