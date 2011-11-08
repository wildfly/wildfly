package org.jboss.as.test.integration.ee.appclient.basic;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface AppClientSingletonRemote {

    public void makeAppClientCall();

    public boolean awaitAppClientCall();
}
