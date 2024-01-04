/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.beanmanager;

import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.weld.util.ForwardingBeanManager;

/**
 * Proxy for Jakarta Contexts and Dependency Injection BeanManager
 *
 * @author Scott Marlow
 */
public class ProxyBeanManager extends ForwardingBeanManager {

    private volatile BeanManager delegate;

    @Override
    public BeanManager delegate() {
        return delegate;
    }

    public void setDelegate(BeanManager delegate) {
        this.delegate = delegate;
    }
}
