package org.jboss.as.test.integration.ejb;

/**
 * Definition of hostname and ports which are used in tests.
 * 
 * @author Ondrej Chaloupka
 */
public class HostnameResolver {
    private static String hostname = "localhost";
    private static Integer managementPort = 9999;
    
    public static String getHostname() {
        return hostname;
    }
    public static void setHostname(String hostname) {
        HostnameResolver.hostname = hostname;
    }
    public static Integer getManagementPort() {
        return managementPort;
    }
    public static void setManagementPort(Integer managementPort) {
        HostnameResolver.managementPort = managementPort;
    }
}
