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
public class MailSubsystem30TestCase extends MailSubsystemTestBase {
    public MailSubsystem30TestCase() {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-mail_3_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/mail.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

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

}
