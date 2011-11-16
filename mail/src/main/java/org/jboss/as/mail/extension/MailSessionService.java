package org.jboss.as.mail.extension;


import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:14
 */
public class MailSessionService implements Service<Session> {
    private static final Logger log = Logger.getLogger(MailSessionService.class);
    private volatile Properties props;
    private final MailSessionConfig config;

    public MailSessionService(MailSessionConfig config) {
        log.trace("service constructed with config: " + config);
        this.config = config;
    }


    public void start(StartContext startContext) throws StartException {
        log.trace("start...");
        props = getProperties();
    }


    public void stop(StopContext stopContext) {
        log.trace("stop...");
    }

    /*
    Name                    Type        Description
    mail.debug              boolean     The initial debug mode. Default is false.
    mail.from               String      The return email address of the current user, used by the InternetAddress method getLocalAddress.
    mail.mime.address.strict boolean    The MimeMessage class uses the InternetAddress method parseHeader to parse headers in messages. This property controls the strict flag passed to the parseHeader method. The default is true.
    mail.host               String      The default host name of the mail server for both Stores and Transports. Used if the mail.protocol.host property isn't set.
    mail.store.protocol     String      Specifies the default message access protocol. The Session method getStore() returns a Store object that implements this protocol. By default the first Store provider in the configuration files is returned.
    mail.transport.protocol String      Specifies the default message transport protocol. The Session method getTransport() returns a Transport object that implements this protocol. By default the first Transport provider in the configuration files is returned.
    mail.user               String      The default user name to use when connecting to the mail server. Used if the mail.protocol.user property isn't set.
    mail.protocol.class     String      Specifies the fully qualified class name of the provider for the specified protocol. Used in cases where more than one provider for a given protocol exists; this property can be used to specify which provider to use by default. The provider must still be listed in a configuration file.
    mail.protocol.host      String      The host name of the mail server for the specified protocol. Overrides the mail.host property.
    mail.protocol.port      int         The port number of the mail server for the specified protocol. If not specified the protocol's default port number is used.
    mail.protocol.user      String      The user name to use when connecting to mail servers using the specified protocol. Overrides the mail.user property.
     */
    private Properties getProperties() {
        Properties props = new Properties();

        if (config.getSmtpServer() != null) {
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.host", config.getSmtpServer().getAddress());
            props.put("mail.smtp.port", config.getSmtpServer().getPort());
        }
        if (config.getImapServer() != null) {
            props.put("mail.imap.host", config.getImapServer().getAddress());
            props.put("mail.imap.port", config.getImapServer().getPort());
        }
        if (config.getPop3Server() != null) {
            props.put("mail.pop3.host", config.getPop3Server().getAddress());
            props.put("mail.pop3.port", config.getPop3Server().getPort());
        }

        props.put("mail.debug", config.isDebug());
        //todo maybe add mail.from

        log.trace("props: " + props);
        return props;
    }

    private void setAuthentication(final Session session) {
        setAuthForServer(session, config.getSmtpServer(), "smtp");
        setAuthForServer(session, config.getPop3Server(), "pop3");
        setAuthForServer(session, config.getImapServer(), "imap");
    }

    private void setAuthForServer(final Session session, final MailSessionServer server, final String protocol) {
        if (server != null) {
            Credentials c = server.getCredentials();
            URLName urlName = new URLName(protocol, server.getAddress(), server.getPort(), "", c != null ? c.getUsername() : null, c != null ? c.getPassword() : null);
            if (c != null) {
                session.setPasswordAuthentication(urlName, new PasswordAuthentication(c.getUsername(), c.getPassword()));
            }
        }
    }


    public Session getValue() throws IllegalStateException, IllegalArgumentException {
        final Session session = Session.getInstance(props);
        setAuthentication(session);
        return session;

    }
}
