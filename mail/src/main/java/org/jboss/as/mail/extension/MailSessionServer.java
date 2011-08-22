package org.jboss.as.mail.extension;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:50
 */
public class MailSessionServer {
    private final String address;
    private final int port;
    private final Credentials credentials;

    public MailSessionServer(String address, int port, Credentials credentials) {
        this.address = address;
        this.port = port;
        this.credentials = credentials;
    }

    public MailSessionServer(String address, int port, String username, String password) {
        this.address = address;
        this.port = port;
        if (username != null) {
            this.credentials = new Credentials(username, password);
        }else{
            credentials = null;
        }
    }


    public String getAddress() {
        return address;
    }


    public int getPort() {
        return port;
    }

    public Credentials getCredentials() {
        return credentials;
    }

}
