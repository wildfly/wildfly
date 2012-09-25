package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

/**
 *
 */
public interface BaseService<T> {
    public void doSomething(T param);
}