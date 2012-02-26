package org.jboss.as.test.shared;

import java.util.List;

/**
 * Class that allows for non arquillian tests to access the current
 * server address and port, and other testsuite environment properties.
 *
 * This should only be used for tests that do not have access to the {@link org.jboss.as.arquillian.container.ManagementClient}
 *
 * @author Stuart Douglas
 */
public class TestSuiteEnvironment {

    /**
     *
     * @return The server port for node0
     */
    public static int getServerPort() {
        return Integer.getInteger("as.managementPort", 9999);
    }

    /**
     *
     * @return The server address of node0
     */
    public static String getServerAddress() {
        return formatPossibleIpv6Address(System.getProperty("node0", "localhost"));
    }

    /**
     *
     * @return The ipv6 arguments that should be used when launching external java processes, such as the application client
     */
    public static String getIpv6Args() {
        if(System.getProperty("ipv6") == null) {
            return " -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ";
        }
        return " -Djava.net.preferIPv4Stack=false -Djava.net.preferIPv6Addresses=true ";
    }

    /**
     *
     */
    public static void getIpv6Args(List<String> command) {
        if(System.getProperty("ipv6") == null) {
            command.add("-Djava.net.preferIPv4Stack=true");
            command.add("-Djava.net.preferIPv6Addresses=false");
        } else {
            command.add("-Djava.net.preferIPv4Stack=false");
            command.add("-Djava.net.preferIPv6Addresses=true");
        }
    }

    public static String formatPossibleIpv6Address(String address) {
        if (address == null) {
            return address;
        }
        if (!address.contains(":")) {
            return address;
        }
        if (address.startsWith("[") && address.endsWith("]")) {
            return address;
        }
        return "[" + address + "]";
    }
}
