package org.jboss.as.mail.extension;

/**
* @author Tomaz Cerar
* @created 10.8.11 22:50
*/
public class MailSessionServer {
    private String address;
    private String port;

    public MailSessionServer() {
    }

    public MailSessionServer(String address, String port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
