package org.jboss.as.test.shared;

import org.jboss.as.network.NetworkUtils;

/**
 * Class that allows for non arquillian tests to access the current
 * server address and port.
 *
 * This should only be used for tests that do not have access to the {@link org.jboss.as.arquillian.container.ManagementClient}
 *
 * @author Stuart Douglas
 */
public class ServerAddress {

    public static int getServerPort() {
        return Integer.getInteger("as.managementPort", 9999);
    }

    public static String getServerAddress() {
        return NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "localhost"));
    }

}
