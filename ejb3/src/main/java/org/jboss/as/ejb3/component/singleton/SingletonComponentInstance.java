/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.singleton;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;

/**
 * @author Jaikiran Pai
 */
public class SingletonComponentInstance extends SessionBeanComponentInstance  {


    /**
     * Construct a new instance.
     *
     * @param component           the component
     */
    public SingletonComponentInstance(final BasicComponent component, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        super(component, preDestroyInterceptor, methodInterceptors);
    }

    @Override
    protected SessionID getId() {
        return null;
    }

}
