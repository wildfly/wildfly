package org.jboss.as.mail.extension;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @created 25.7.11 15:48
 */
public class MailSessionConfig {
    private String jndiName;
    private boolean debug = false;
    private String from = null;

    private MailSessionServer smtpServer;
    private MailSessionServer pop3Server;
    private MailSessionServer imapServer;


    protected MailSessionConfig() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getJndiName() {
        return jndiName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public MailSessionServer getImapServer() {
        return imapServer;
    }

    public void setImapServer(MailSessionServer imapServer) {
        this.imapServer = imapServer;
    }

    public MailSessionServer getPop3Server() {
        return pop3Server;
    }

    public void setPop3Server(MailSessionServer pop3Server) {
        this.pop3Server = pop3Server;
    }

    public MailSessionServer getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(MailSessionServer smtpServer) {
        this.smtpServer = smtpServer;
    }

    @Override
    public String toString() {
        return "MailSessionConfig{" +
                "imapServer=" + imapServer +
                ", jndiName='" + jndiName + '\'' +
                ", smtpServer=" + smtpServer +
                ", pop3Server=" + pop3Server +
                '}';
    }
}
