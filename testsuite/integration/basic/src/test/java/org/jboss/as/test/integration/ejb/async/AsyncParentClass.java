package org.jboss.as.test.integration.ejb.async;

import javax.ejb.Asynchronous;

/**
 * @author Ondrej Chaloupka
 */
@Asynchronous
public class AsyncParentClass {
    public static volatile boolean voidMethodCalled = false;
}
