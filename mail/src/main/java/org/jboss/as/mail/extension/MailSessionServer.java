package org.jboss.as.mail.extension;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:50
 */
public class MailSessionServer {
    private final String address;
    private final String port;

    public MailSessionServer(String address, String port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }


    public String getPort() {
        return port;
    }


}
