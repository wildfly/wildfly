package org.jboss.as.test.shared;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.commons.lang.StringUtils;

import org.jboss.as.arquillian.container.Authentication;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 * Class that allows for non arquillian tests to access the current server address and port, and other testsuite environment
 * properties.
 * <p/>
 * This should only be used for tests that do not have access to the {@link org.jboss.as.arquillian.container.ManagementClient}
 *
 * @author Stuart Douglas
 */
public class TestSuiteEnvironment {

    public static ModelControllerClient getModelControllerClient() {
        try {
            return ModelControllerClient.Factory.create(
                    InetAddress.getByName(getServerAddress()),
                    TestSuiteEnvironment.getServerPort(),
                    Authentication.getCallbackHandler()
                    );
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJavaPath() {
        String home = System.getenv("JAVA_HOME");
        if(home == null) {
            home = getSystemProperty("java.home");
        }
        if(home != null) {
            return home + java.io.File.separator + "bin" + java.io.File.separator + "java";
        }
        return "java";
    }

    public static String getSystemProperty(String name, String def) {
        return System.getProperty(name, def);
    }

    public static String getSystemProperty(String name) {
        return System.getProperty(name);
    }

    public static String getTmpDir() {
        return getSystemProperty("java.io.tmpdir");
    }

    /**
     * @return The server port for node0
     */
    public static int getServerPort() {
        return Integer.getInteger("as.managementPort", 9999);
    }

    /**
     * @return The server address of node0
     */
    public static String getServerAddress() {
        return formatPossibleIpv6Address(System.getProperty("node0", "localhost"));
    }

    /**
     * @return The ipv6 arguments that should be used when launching external java processes, such as the application client
     */
    public static String getIpv6Args() {
        if (System.getProperty("ipv6") == null) {
            return " -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ";
        }
        return " -Djava.net.preferIPv4Stack=false -Djava.net.preferIPv6Addresses=true ";
    }

    /**
     *
     */
    public static void getIpv6Args(List<String> command) {
        if (System.getProperty("ipv6") == null) {
            command.add("-Djava.net.preferIPv4Stack=true");
            command.add("-Djava.net.preferIPv6Addresses=false");
        } else {
            command.add("-Djava.net.preferIPv4Stack=false");
            command.add("-Djava.net.preferIPv6Addresses=true");
            command.add("-Djboss.default.multicast.address="
                    + (System.getProperty("udpGroup") != null ? System.getProperty("udpGroup") : (System.getProperty("mcast") != null ? System.getProperty("mcast") : "ff01::1")));

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

    public static String getSecondaryTestAddress(final boolean useCannonicalHost) {
        String address = System.getProperty("secondary.test.address");
        if (StringUtils.isBlank(address)) {
            address = getServerAddress();
        }
        if (useCannonicalHost) {
            address = StringUtils.strip(address, "[]");
        }
        return address;
    }
}
