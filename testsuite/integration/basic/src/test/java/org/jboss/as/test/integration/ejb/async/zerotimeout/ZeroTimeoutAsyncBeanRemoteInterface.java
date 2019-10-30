package org.jboss.as.test.integration.ejb.async.zerotimeout;

import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import java.util.concurrent.Future;

/**
 * @author Daniel Cihak
 */
@Remote
@Asynchronous
public interface ZeroTimeoutAsyncBeanRemoteInterface {

    Future<Boolean> futureMethod() throws InterruptedException;
}
