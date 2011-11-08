package org.jboss.as.test.integration.ee.appclient.basic;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface AppClientSingletonRemote {

    public void reset();

    public void makeAppClientCall(final String value);

    public String awaitAppClientCall();
}
