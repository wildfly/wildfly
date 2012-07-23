package org.jboss.as.mail.extension;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:50
 */
public class MailSessionServer {
    private final String outgoingSocketBinding;
    private final Credentials credentials;
    private boolean sslEnabled = false;
    private boolean tlsEnabled = false;

    public MailSessionServer(final String outgoingSocketBinding, final Credentials credentials, boolean ssl,boolean tls) {
        this.outgoingSocketBinding = outgoingSocketBinding;
        this.credentials = credentials;
        this.sslEnabled = ssl;
        this.tlsEnabled = tls;
    }

    public String getOutgoingSocketBinding() {
        return outgoingSocketBinding;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }
}
