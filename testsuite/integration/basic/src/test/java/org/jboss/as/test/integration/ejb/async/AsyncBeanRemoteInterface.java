package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Remote;

/**
 * @author Ondrej Chaloupka
 */
@Remote
public interface AsyncBeanRemoteInterface {
    public void asyncMethod() throws InterruptedException;
    public Future<Boolean> futureMethod() throws InterruptedException, ExecutionException;
}
