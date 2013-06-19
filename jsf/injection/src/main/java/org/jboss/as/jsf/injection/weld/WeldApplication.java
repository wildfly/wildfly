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
package org.jboss.as.jsf.injection.weld;

import org.jboss.weld.el.WeldELContextListener;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author pmuir
 */
public class WeldApplication extends ForwardingApplication {

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
        if (!initialized &&  beanManager() != null) {
            synchronized (this) {
                if(!initialized &&  beanManager() != null) {
                    elResolver.setDelegate(beanManager().getELResolver());
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
        init();
        // may be improved for thread safety, but right now the only risk is to invoke wrapExpressionFactory
        // multiple times for concurrent threads. This is ok, as the call is
        if (expressionFactory == null) {
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