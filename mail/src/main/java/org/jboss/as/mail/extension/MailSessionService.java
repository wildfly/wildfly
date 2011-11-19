package org.jboss.as.mail.extension;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;

import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that provides a javax.mail.Session.
 *
 * @author Tomaz Cerar
 * @created 27.7.11 0:14
 */
public class MailSessionService implements Service<Session> {
    private static final Logger log = Logger.getLogger(MailSessionService.class);
    private volatile Properties props;
    private final MailSessionConfig config;
    private Map<String, OutboundSocketBinding> socketBindings = new HashMap<String, OutboundSocketBinding>();

    public MailSessionService(MailSessionConfig config) {
        log.tracef("service constructed with config: %s", config);
        this.config = config;
    }


    public void start(StartContext startContext) throws StartException {
        log.trace("start...");
        props = getProperties();
    }


    public void stop(StopContext stopContext) {
        log.trace("stop...");
    }

    Injector<OutboundSocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, OutboundSocketBinding>(socketBindings, name);
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
    private Properties getProperties() throws StartException  {
        Properties props = new Properties();

        if (config.getSmtpServer() != null) {
            props.put("mail.transport.protocol", "smtp");
            InetSocketAddress socketAddress = getServerSocketAddress(config.getSmtpServer());
            props.put(getHostKey("smtp"), socketAddress.getAddress().getHostName());
            props.put(getPortKey("smtp"), socketAddress.getPort());
        }
        if (config.getImapServer() != null) {
            InetSocketAddress socketAddress = getServerSocketAddress(config.getImapServer());
            props.put(getHostKey("imap"), socketAddress.getAddress().getHostName());
            props.put(getPortKey("imap"), socketAddress.getPort());
        }
        if (config.getPop3Server() != null) {
            InetSocketAddress socketAddress = getServerSocketAddress(config.getPop3Server());
            props.put(getHostKey("pop3"), socketAddress.getAddress().getHostName());
            props.put(getPortKey("pop3"), socketAddress.getPort());
        }

        props.put("mail.debug", config.isDebug());
        //todo maybe add mail.from

        log.tracef("props: %s", props);

        return props;
    }

   private void setAuthentication(final Session session) {
        setAuthForServer(session, config.getSmtpServer(), "smtp");
        setAuthForServer(session, config.getPop3Server(), "pop3");
        setAuthForServer(session, config.getImapServer(), "imap");
    }

    private void setAuthForServer(final Session session, final MailSessionServer server, final String protocol) {
        if (server != null) {
            final String host = String.class.cast(props.get(getHostKey(protocol)));
            final int port = Integer.class.cast(props.get(getPortKey(protocol)));
            Credentials c = server.getCredentials();
            URLName urlName = new URLName(protocol, host, port, "", c != null ? c.getUsername() : null, c != null ? c.getPassword() : null);
            if (c != null) {
                session.setPasswordAuthentication(urlName, new PasswordAuthentication(c.getUsername(), c.getPassword()));
            }
        }
    }

    private InetSocketAddress getServerSocketAddress(MailSessionServer server) throws StartException {
        final String ref = server.getOutgoingSocketBinding();
        final OutboundSocketBinding binding = socketBindings.get(ref);
        if (ref == null) {
            throw MailMessages.MESSAGES.outboundSocketBindingNotAvailable(ref);
        }
        final InetAddress destinationAddress;
        try {
            destinationAddress = binding.getDestinationAddress();
        } catch (UnknownHostException uhe) {
            throw MailMessages.MESSAGES.unknownOutboundSocketBindingDesintation(uhe, ref);
        }
        return new InetSocketAddress(destinationAddress, binding.getDestinationPort());
    }


    public Session getValue() throws IllegalStateException, IllegalArgumentException {
        final Session session = Session.getInstance(props);
        setAuthentication(session);
        return session;
    }

    private static String getHostKey(final String protocol) {
        return new StringBuilder("mail.").append(protocol).append(".host").toString();
    }

    private static String getPortKey(final String protocol) {
        return new StringBuilder("mail.").append(protocol).append(".port").toString();
    }
}
