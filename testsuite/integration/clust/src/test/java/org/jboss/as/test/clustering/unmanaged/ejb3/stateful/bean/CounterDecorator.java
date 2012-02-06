package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.bean;

import java.io.Serializable;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Decorator
public class CounterDecorator implements Serializable, Counter {

    @Inject
    @Delegate
    private Counter counter;


    @Override
    public int getCount() {
        return counter.getCount() + 10000000;
    }
}
