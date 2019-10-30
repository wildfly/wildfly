package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

/**
 *
 */
public interface BaseService<T> {
    void doSomething(T param);
}