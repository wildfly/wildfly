package org.jboss.as.mail.extension;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Session;

import org.jboss.as.network.OutboundSocketBinding;
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
    private volatile Properties props;
    private final MailSessionConfig config;
    private Map<String, OutboundSocketBinding> socketBindings = new HashMap<String, OutboundSocketBinding>();

    public MailSessionService(MailSessionConfig config) {
        MailLogger.ROOT_LOGGER.tracef("service constructed with config: %s", config);
        this.config = config;
    }

    public MailSessionConfig getConfig() {
        return config;
    }

    public void start(StartContext startContext) throws StartException {
        MailLogger.ROOT_LOGGER.trace("start...");
        props = getProperties();
    }


    public void stop(StopContext stopContext) {
        MailLogger.ROOT_LOGGER.trace("stop...");
    }

    Injector<OutboundSocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, OutboundSocketBinding>(socketBindings, name);
    }

    /**
     * Configures mail session properties
     *
     * @return Properties for session
     * @throws StartException if socket binding could not be found
     * @see {http://javamail.kenai.com/nonav/javadocs/com/sun/mail/smtp/package-summary.html}
     * @see {http://javamail.kenai.com/nonav/javadocs/com/sun/mail/pop3/package-summary.html}
     * @see {http://javamail.kenai.com/nonav/javadocs/com/sun/mail/imap/package-summary.html}
     */
    private Properties getProperties() throws StartException {
        Properties props = new Properties();

        if (config.getSmtpServer() != null) {
            props.setProperty("mail.transport.protocol", "smtp");
            setServerProps(props, config.getSmtpServer(), "smtp");
        }
        if (config.getImapServer() != null) {
            setServerProps(props, config.getImapServer(), "imap");
        }
        if (config.getPop3Server() != null) {
            setServerProps(props, config.getPop3Server(), "pop3");
        }
        if (config.getCustomServers() != null) {
            configureCustomServers(props, config.getCustomServers());
        }
        if (config.getFrom() != null) {
            props.setProperty("mail.from", config.getFrom());
        }
        props.setProperty("mail.debug", String.valueOf(config.isDebug()));
        MailLogger.ROOT_LOGGER.tracef("props: %s", props);
        return props;
    }

    private void configureCustomServers(final Properties props, final CustomServerConfig... serverConfigs) throws StartException {
        for (CustomServerConfig config : serverConfigs) {
            setServerProps(props, config, config.getProtocol());
        }
    }

    private void setServerProps(final Properties props, final ServerConfig server, final String protocol) throws StartException {
        if (server.isSslEnabled()) {
            props.setProperty(getPropKey(protocol, "ssl.enable"), "true");
        } else if (server.isTlsEnabled()) {
            props.setProperty(getPropKey(protocol, "starttls.enable"), "true");
        }
        if (server.getCredentials() != null) {
            props.setProperty(getPropKey(protocol, "auth"), "true");
            props.setProperty(getPropKey(protocol, "user"), server.getCredentials().getUsername());
        }
        props.setProperty(getPropKey(protocol, "debug"), String.valueOf(config.isDebug()));


        Map<String, String> customProps = server.getProperties();
        if (server.getOutgoingSocketBinding() != null) {
            InetSocketAddress socketAddress = getServerSocketAddress(server);
            props.setProperty(getHostKey(protocol), socketAddress.getAddress().getHostName());
            props.setProperty(getPortKey(protocol), String.valueOf(socketAddress.getPort()));
        } else {
            String host = customProps.get("host");
            if (host != null && !"".equals(host.trim())) {
                props.setProperty(getHostKey(protocol), host);
            }
            String port = customProps.get("port");
            if (port != null && !"".equals(port.trim())) {
                props.setProperty(getPortKey(protocol), port);
            }
        }
        if (customProps!=null&&!customProps.isEmpty()) {
            for (Map.Entry<String, String> prop : customProps.entrySet()) {
                if (!props.contains(prop.getKey())) {
                    props.put(getPropKey(protocol,prop.getKey()), prop.getValue());
                }
            }
        }
    }

    private InetSocketAddress getServerSocketAddress(ServerConfig server) throws StartException {
        final String ref = server.getOutgoingSocketBinding();
        final OutboundSocketBinding binding = socketBindings.get(ref);
        if (ref == null) {
            throw MailMessages.MESSAGES.outboundSocketBindingNotAvailable(ref);
        }
        final InetAddress destinationAddress;
        try {
            destinationAddress = binding.getDestinationAddress();
        } catch (UnknownHostException uhe) {
            throw MailMessages.MESSAGES.unknownOutboundSocketBindingDestination(uhe, ref);
        }
        return new InetSocketAddress(destinationAddress, binding.getDestinationPort());
    }


    public Session getValue() throws IllegalStateException, IllegalArgumentException {
        final Session session = Session.getInstance(props, new PasswordAuthentication());
        return session;
    }


    private static String getHostKey(final String protocol) {
        return new StringBuilder("mail.").append(protocol).append(".host").toString();
    }

    private static String getPortKey(final String protocol) {
        return new StringBuilder("mail.").append(protocol).append(".port").toString();
    }

    private static String getPropKey(final String protocol, final String name) {
        return new StringBuilder("mail.").append(protocol).append(".").append(name).toString();
    }

    /*
     * for testing purposes only!
     * @param session
     */
    /*private static void sendMail(Session session) {
        Message msg = new MimeMessage(session);
        try {
            InternetAddress addressFrom = new InternetAddress("tomaz@cerar.net");
            msg.setFrom(addressFrom);
            msg.setRecipients(Message.RecipientType.TO, new Address[]{new InternetAddress("tomaz.cerar@gmail.com")});
            msg.setSubject("Test Jboss AS7 mail subsystem");
            msg.setContent("Testing mail subsystem, loerm ipsum", "text/plain");
            Transport.send(msg);
        } catch (Exception e) {
            // Needs i18n if using
            log.error("could not send mail", e);
        }
    }*/

    /**
     * @author Tomaz Cerar
     * @created 5.12.11 16:22
     */
    public class PasswordAuthentication extends javax.mail.Authenticator {
        @Override
        protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
            String protocol = getRequestingProtocol();
            Credentials c = null;
            if (MailSubsystemModel.SMTP.equals(protocol)) {
                c = config.getSmtpServer().getCredentials();
            } else if (MailSubsystemModel.POP3.equals(protocol)) {
                c = config.getPop3Server().getCredentials();
            } else if (MailSubsystemModel.IMAP.equals(protocol)) {
                c = config.getImapServer().getCredentials();
            }

            if (c != null) {
                return new javax.mail.PasswordAuthentication(c.getUsername(), c.getPassword());
            }
            return null;
        }
    }
}
