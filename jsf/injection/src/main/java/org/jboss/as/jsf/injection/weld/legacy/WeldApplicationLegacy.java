/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.injection.weld.legacy;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.application.Application;
import org.jboss.as.jsf.injection.weld.DummyELResolver;
import org.jboss.as.jsf.injection.weld.ForwardingELResolver;
import org.jboss.weld.module.web.el.WeldELContextListener;

/**
 * @author pmuir
 *
 * Bring this class back to allow Faces 1.2 to be used with WildFly
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
