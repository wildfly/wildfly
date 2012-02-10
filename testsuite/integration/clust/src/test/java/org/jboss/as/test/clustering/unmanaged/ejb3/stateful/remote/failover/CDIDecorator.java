package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.remote.failover;

import java.io.Serializable;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Decorator
public abstract class CDIDecorator implements DecoratorInterface, Serializable {

    @Delegate
    @Inject
    private DecoratorInterface delegate;

    @Override
    public String getMessage() {
        return  "Hello " + delegate.getMessage();
    }
}
