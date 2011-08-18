package org.jboss.as.mail.extension;


import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import javax.mail.Session;
import java.util.Properties;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:14
 */
public class MailSessionService implements Service<Session> {
    private static final Logger log = Logger.getLogger(MailSessionService.class);
    private Session session;
    private final MailSessionConfig sessionConfig;

    public MailSessionService(MailSessionConfig sessionConfig) {
        log.trace("service constructed with config: " + sessionConfig);
        this.sessionConfig = sessionConfig;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        log.trace("start...");
        session = Session.getDefaultInstance(getProperties());
        log.trace("session is: " + session);
    }

    @Override
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
        props.put("mail.transport.protocol", "smtp");
        if (sessionConfig.getSmtpServer() != null) {
            props.put("mail.smtp.host", sessionConfig.getSmtpServer().getAddress());
            props.put("mail.smtp.port", sessionConfig.getSmtpServer().getPort());
        }
        if (sessionConfig.getImapServer() != null) {
            props.put("mail.imap.host", sessionConfig.getImapServer().getAddress());
            props.put("mail.imap.port", sessionConfig.getImapServer().getPort());
        }
        if (sessionConfig.getPop3Server() != null) {
            props.put("mail.pop3.host", sessionConfig.getPop3Server().getAddress());
            props.put("mail.pop3.port", sessionConfig.getPop3Server().getPort());
        }

        props.put("mail.user", sessionConfig.getUsername());
        props.put("mail.debug", sessionConfig.isDebug());
        //todo maybe add mail.from

        log.trace("props: " + props);
        return props;
    }


    @Override
    public Session getValue() throws IllegalStateException, IllegalArgumentException {
        return session;

    }
}
