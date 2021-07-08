/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.mail.extension;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.msc.service.ServiceController;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class MailSubsystem40TestCase extends MailSubsystemTestBase {
    public MailSubsystem40TestCase() {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_4_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-mail_4_0.xsd";
    }

    /**
     * Tests that runtime information is the expected one based on the subsystem_4_0.xml subsystem configuration.
     *
     * @throws Exception
     */
    @Test
    public void testRuntime() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new DefaultInitializer())
                .setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
        }
        ServiceController<?> javaMailService = mainServices.getContainer().getService(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("defaultMail"));
        javaMailService.setMode(ServiceController.Mode.ACTIVE);
        Session session = (Session) javaMailService.getValue();
        Assert.assertNotNull("session should not be null", session);
        Properties properties = session.getProperties();
        Assert.assertNotNull("smtp host should be set", properties.getProperty("mail.smtp.host"));
        Assert.assertNotNull("pop3 host should be set", properties.getProperty("mail.pop3.host"));
        Assert.assertNotNull("imap host should be set", properties.getProperty("mail.imap.host"));
        PasswordAuthentication auth = session.requestPasswordAuthentication(InetAddress.getLocalHost(), 25, "smtp", "", "");
        Assert.assertEquals("nobody", auth.getUserName());
        Assert.assertEquals("pass", auth.getPassword());

        ServiceController<?> defaultMailService = mainServices.getContainer().getService(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("default2"));
        session = (Session) defaultMailService.getValue();
        Assert.assertEquals("Debug should be true", true, session.getDebug());


        ServiceController<?> customMailService = mainServices.getContainer().getService(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("custom"));
        session = (Session) customMailService.getValue();
        properties = session.getProperties();
        String host = properties.getProperty("mail.smtp.host");
        Assert.assertNotNull("smtp host should be set", host);
        Assert.assertEquals("mail.example.com", host);

        Assert.assertEquals("localhost", properties.get("mail.pop3.host")); //this one should be read out of socket binding
        Assert.assertEquals("some-custom-prop-value", properties.get("mail.pop3.custom_prop")); //this one should be extra property
        Assert.assertEquals("fully-qualified-prop-name", properties.get("some.fully.qualified.property")); //this one should be extra property

        MailSessionService service = (MailSessionService) customMailService.getService();
        Credentials credentials = service.getConfig().getCustomServers()[0].getCredentials();
        Assert.assertEquals(credentials.getUsername(), "username");
        Assert.assertEquals(credentials.getPassword(), "password");

    }

    /**
     * Tests that runtime information coming from attribute expressions is the expected one based on the subsystem_4_0.xml subsystem configuration.
     *
     * @throws Exception
     */
    @Test
    public void testExpressionsRuntime() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new DefaultInitializer())
                .setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
        }

        ServiceController<?> defaultMailSession3 = mainServices.getContainer().getService(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("default3"));
        defaultMailSession3.setMode(ServiceController.Mode.ACTIVE);

        MailSessionService mailService = (MailSessionService) defaultMailSession3.getService();
        MailSessionConfig config = mailService.getConfig();
        Assert.assertEquals("Unexpected value for mail-session=default3 from attribute", "from@from.org", config.getFrom());
        Assert.assertEquals("Unexpected value for mail-session=default3 jndi-name attribute", "java:jboss/mail/Default3", config.getJndiName());
        Assert.assertEquals("Unexpected value for mail-session=default3 debug attribute", Boolean.TRUE, config.isDebug());

        ServerConfig smtpServerConfig = config.getSmtpServer();
        Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/outbound-socket-binding-ref attribute", "mail-smtp", smtpServerConfig.getOutgoingSocketBinding());
        Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/tls attribute", Boolean.TRUE, smtpServerConfig.isTlsEnabled());
        Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/ssl attribute", Boolean.FALSE, smtpServerConfig.isSslEnabled());

        Credentials credentials = smtpServerConfig.getCredentials();
        Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/username attribute", "nobody", credentials.getUsername());
        Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/password attribute", "empty", credentials.getPassword());

        ServiceController<?> customMailService3 = mainServices.getContainer().getService(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("custom3"));
        customMailService3.setMode(ServiceController.Mode.ACTIVE);
        mailService = (MailSessionService) customMailService3.getService();
        config = mailService.getConfig();
        CustomServerConfig customServerConfig = config.getCustomServers()[0];
        Map<String, String> properties = customServerConfig.getProperties();
        Assert.assertEquals("Unexpected value for mail-session=custom3 custom-server/property value attribute", "mail.example.com", properties.get("host"));
    }

}
