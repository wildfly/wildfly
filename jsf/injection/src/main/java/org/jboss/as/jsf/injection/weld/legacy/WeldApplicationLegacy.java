/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jsf.injection.weld.legacy;

import org.jboss.as.jsf.injection.weld.DummyELResolver;
import org.jboss.as.jsf.injection.weld.ForwardingELResolver;
import org.jboss.weld.module.web.el.WeldELContextListener;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author pmuir
 *
 * Bring this class back to allow JSF 1.2 to be used with WildFly
 * See https://issues.jboss.org/browse/WFLY-9708
 */
public class WeldApplicationLegacy extends ForwardingApplication {

    private static class AdjustableELResolver extends ForwardingELResolver {

        private ELResolver delegate;

        public void setDelegate(ELResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        protected ELResolver delegate() {
            return delegate;
        }
    }

    private final Application application;
    private final AdjustableELResolver elResolver;

    private volatile ExpressionFactory expressionFactory;
    private volatile boolean initialized = false;
    private volatile BeanManager beanManager;

    public WeldApplicationLegacy(Application application) {
        this.application = application;
        application.addELContextListener(new WeldELContextListener());
        elResolver = new AdjustableELResolver();
        elResolver.setDelegate(new DummyELResolver());
        application.addELResolver(elResolver);
    }

    private void init() {
        if (!initialized) {
            synchronized (this) {
                if(!initialized) {
                    if(beanManager() != null) {
                        elResolver.setDelegate(beanManager().getELResolver());
                    }
                    initialized = true;
                }
            }
        }
    }

    @Override
    protected Application delegate() {
        init();
        return application;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        // may be improved for thread safety, but right now the only risk is to invoke wrapExpressionFactory
        // multiple times for concurrent threads. This is ok, as the call is
        if (expressionFactory == null) {
            init();
            synchronized (this) {
                if (expressionFactory == null) {
                    BeanManager bm = beanManager();
                    if (bm == null) {
                        expressionFactory = application.getExpressionFactory();
                    } else {
                        expressionFactory = bm.wrapExpressionFactory(application.getExpressionFactory());
                    }
                }
            }
        }
        return expressionFactory;
    }

    private BeanManager beanManager() {
        if (beanManager == null) {
            synchronized (this) {
                if (beanManager == null) {
                    try {
                        // This can throw IllegalArgumentException on servlet context destroyed if init() was never called
                        beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
                    } catch (NamingException | IllegalArgumentException e) {
                        return null;
                    }
                }
            }
        }
        return beanManager;
    }
}
