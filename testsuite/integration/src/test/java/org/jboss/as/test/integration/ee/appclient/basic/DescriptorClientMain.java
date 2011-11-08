package org.jboss.as.test.integration.ee.appclient.basic;

/**
 * @author Stuart Douglas
 */
public class DescriptorClientMain {

    private static AppClientSingletonRemote appClientSingletonRemote;

    private static String envEntry;

    public static void main(final String[] params) {
        appClientSingletonRemote.makeAppClientCall(envEntry);
    }

}
