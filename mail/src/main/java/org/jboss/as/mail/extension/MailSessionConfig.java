package org.jboss.as.mail.extension;

import java.util.Arrays;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @created 25.7.11 15:48
 */
class MailSessionConfig {
    private String jndiName;
    private boolean debug = false;
    private String from = null;

    private ServerConfig smtpServer;
    private ServerConfig pop3Server;
    private ServerConfig imapServer;
    private CustomServerConfig[] customServers = new CustomServerConfig[0];


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

    public ServerConfig getImapServer() {
        return imapServer;
    }

    public void setImapServer(ServerConfig imapServer) {
        this.imapServer = imapServer;
    }

    public ServerConfig getPop3Server() {
        return pop3Server;
    }

    public void setPop3Server(ServerConfig pop3Server) {
        this.pop3Server = pop3Server;
    }

    public ServerConfig getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(ServerConfig smtpServer) {
        this.smtpServer = smtpServer;
    }

    public CustomServerConfig[] getCustomServers() {
        return customServers;
    }

    public void setCustomServers(CustomServerConfig... customServer) {
        this.customServers = customServer;
    }

    public void addCustomServer(CustomServerConfig customServer) {
        final int i = customServers.length;
        customServers = Arrays.copyOf(customServers, i + 1);
        customServers[i] = customServer;
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
