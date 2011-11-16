package org.jboss.as.mail.extension;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:50
 */
public class MailSessionServer {
    private final String outgoingSocketBinding;
    private final Credentials credentials;

    public MailSessionServer(final String outgoingSocketBinding, final Credentials credentials) {
        this.outgoingSocketBinding = outgoingSocketBinding;
        this.credentials = credentials;
    }

    public MailSessionServer(final String outgoingSocketBinding, String username, String password) {
        this.outgoingSocketBinding = outgoingSocketBinding;
        if (username != null) {
            this.credentials = new Credentials(username, password);
        }else{
            credentials = null;
        }
    }

    public String getOutgoingSocketBinding() {
        return outgoingSocketBinding;
    }

    public Credentials getCredentials() {
        return credentials;
    }

}
