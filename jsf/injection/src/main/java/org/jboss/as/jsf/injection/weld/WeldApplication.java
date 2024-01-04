/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.injection.weld;

import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationWrapper;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.weld.module.web.el.WeldELContextListener;

/**
 * @author pmuir
 */
public class WeldApplication extends ApplicationWrapper {

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

    public WeldApplication(Application application) {
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
    public Application getWrapped() {
        init();
        return application;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
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
