package org.jboss.as.jpa.beanmanager;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.util.ForwardingBeanManager;

/**
 * Proxy for CDI BeanManager
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
