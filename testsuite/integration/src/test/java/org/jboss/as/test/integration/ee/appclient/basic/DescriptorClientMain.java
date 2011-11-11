package org.jboss.as.test.integration.ee.appclient.basic;

import javax.ejb.EJB;

/**
 * @author Stuart Douglas
 */
public class DescriptorClientMain {

    @EJB
    private static AppClientSingletonRemote appClientSingletonRemote;

    private static String envEntry;

    public static void main(final String[] params) {
        appClientSingletonRemote.makeAppClientCall(envEntry);
    }

}
