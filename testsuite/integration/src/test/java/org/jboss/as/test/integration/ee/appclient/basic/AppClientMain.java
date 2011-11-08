package org.jboss.as.test.integration.ee.appclient.basic;

import javax.ejb.EJB;

/**
 * @author Stuart Douglas
 */
public class AppClientMain {

    @EJB
    private static AppClientSingletonRemote appClientSingletonRemote;

    public static void main(final String[] params) {
        appClientSingletonRemote.makeAppClientCall(params[0]);
    }

}
