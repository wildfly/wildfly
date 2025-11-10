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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
                        // Test for CDI 4.1+, if present use the new ElAwareBeanManager
                        // This block can be replaced by non-reflective variant soon as WFLY runs CDI 4.1+
                        try {
                            Class<?> elAwareBmClass = Class.forName("jakarta.enterprise.inject.spi.el.ELAwareBeanManager");
                            Method getELResolver = elAwareBmClass.getMethod("getELResolver");
                            elResolver.setDelegate((ELResolver) getELResolver.invoke(elAwareBmClass.cast(beanManager())));
                        } catch (ClassNotFoundException e) {
                            // this is CDI 4.0, use the standard BM
                            elResolver.setDelegate(beanManager().getELResolver());
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
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
                        // Test for CDI 4.1+, if present use the new ElAwareBeanManager
                        // This block can be replaced by non-reflective variant soon as WFLY runs CDI 4.1+
                        try {
                            Class<?> elAwareBmClass = Class.forName("jakarta.enterprise.inject.spi.el.ELAwareBeanManager");
                            Method wrapExpressionFactory = elAwareBmClass.getMethod("wrapExpressionFactory", ExpressionFactory.class);
                            expressionFactory = (ExpressionFactory) wrapExpressionFactory.invoke(elAwareBmClass.cast(bm), application.getExpressionFactory());

                        } catch (ClassNotFoundException e) {
                            // this is CDI 4.0, use the standard BM
                            expressionFactory = bm.wrapExpressionFactory(application.getExpressionFactory());
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
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
