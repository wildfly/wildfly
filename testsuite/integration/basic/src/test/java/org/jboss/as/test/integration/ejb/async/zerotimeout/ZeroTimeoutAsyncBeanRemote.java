package org.jboss.as.test.integration.ejb.async.zerotimeout;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import java.util.concurrent.Future;

/**
 * Bean with asynchronous methods.
 *
 * @author Daniel Cihak
 */
@Stateless
public class ZeroTimeoutAsyncBeanRemote implements ZeroTimeoutAsyncBeanRemoteInterface {

    @Override
    @Asynchronous
    public Future<Boolean> futureMethod() throws InterruptedException {
        Thread.sleep(5000);
        return new AsyncResult<Boolean>(Boolean.TRUE);
    }
}
