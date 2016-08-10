package org.jboss.as.test.integration.ee.appclient.basic;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface AppClientSingletonRemote {

    void reset();

    void makeAppClientCall(final String value);

    String awaitAppClientCall();
}
