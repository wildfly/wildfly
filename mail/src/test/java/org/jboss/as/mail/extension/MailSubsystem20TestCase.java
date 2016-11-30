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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.Properties;
import javax.mail.Session;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class MailSubsystem20TestCase extends AbstractSubsystemBaseTest {
    public MailSubsystem20TestCase() {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_2_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-mail_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/mail.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("subsystem_1_1_expressions.xml", false);
    }

    @Test
    public void test11() throws Exception {
        standardSubsystemTest("subsystem_1_1.xml", false);
    }

    @Test
    public void test12() throws Exception {
        standardSubsystemTest("subsystem_1_2.xml", false);
    }

    @Test
    public void testRuntime() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new MailSubsystem10TestCase.Initializer())
                .setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
        }
        ServiceController<?> javaMailService = mainServices.getContainer().getService(MailSessionAdd.MAIL_SESSION_SERVICE_NAME.append("defaultMail"));
        javaMailService.setMode(ServiceController.Mode.ACTIVE);
        Session session = (Session) javaMailService.getValue();
        Assert.assertNotNull("session should not be null", session);
        Properties properties = session.getProperties();
        Assert.assertNotNull("smtp host should be set", properties.getProperty("mail.smtp.host"));
        Assert.assertNotNull("pop3 host should be set", properties.getProperty("mail.pop3.host"));
        Assert.assertNotNull("imap host should be set", properties.getProperty("mail.imap.host"));

        ServiceController<?> defaultMailService = mainServices.getContainer().getService(MailSessionAdd.MAIL_SESSION_SERVICE_NAME.append("default2"));
        session = (Session) defaultMailService.getValue();
        Assert.assertEquals("Debug should be true", true, session.getDebug());


        ServiceController<?> customMailService = mainServices.getContainer().getService(MailSessionAdd.MAIL_SESSION_SERVICE_NAME.append("custom"));
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


    @Test
    public void testOperations() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new MailSubsystem10TestCase.Initializer())
                .setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
        }

        PathAddress sessionAddress = PathAddress.pathAddress(MailExtension.SUBSYSTEM_PATH, PathElement.pathElement(MailExtension.MAIL_SESSION_PATH.getKey(), "defaultMail"));
        ModelNode result;

        ModelNode removeServerOp = Util.createRemoveOperation(sessionAddress.append("server", "imap"));
        removeServerOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = mainServices.executeOperation(removeServerOp);
        checkResult(result);

        ModelNode addServerOp = Util.createAddOperation(sessionAddress.append("server", "imap"));
        addServerOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        addServerOp.get("outbound-socket-binding-ref").set("mail-imap");
        addServerOp.get("username").set("user");
        addServerOp.get("password").set("pswd");

        result = mainServices.executeOperation(addServerOp);
        checkResult(result);

        checkResult(mainServices.executeOperation(removeServerOp)); //to make sure noting is left behind
        checkResult(mainServices.executeOperation(addServerOp));

        ModelNode writeOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, sessionAddress);
        writeOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        writeOp.get("name").set("debug");
        writeOp.get("value").set(false);
        result = mainServices.executeOperation(writeOp);
        checkResult(result);


        ServiceController<?> javaMailService = mainServices.getContainer().getService(MailSessionAdd.MAIL_SESSION_SERVICE_NAME.append("defaultMail"));
        javaMailService.setMode(ServiceController.Mode.ACTIVE);
        Session session = (Session) javaMailService.getValue();
        Assert.assertNotNull("session should not be null", session);
        Properties properties = session.getProperties();
        Assert.assertNotNull("smtp host should be set", properties.getProperty("mail.smtp.host"));
        Assert.assertNotNull("imap host should be set", properties.getProperty("mail.imap.host"));


        PathAddress nonExisting = PathAddress.pathAddress(MailExtension.SUBSYSTEM_PATH, PathElement.pathElement(MailExtension.MAIL_SESSION_PATH.getKey(), "non-existing-session"));
        ModelNode addSession = Util.createAddOperation(nonExisting);
        addSession.get("jndi-name").set("java:/bah");
        checkResult(mainServices.executeOperation(addSession));
        removeServerOp = Util.createRemoveOperation(nonExisting.append("server", "imap"));
        //removeServerOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = mainServices.executeOperation(removeServerOp);
        checkForFailure(result);



    }

    private ModelNode checkForFailure(ModelNode rsp) throws OperationFailedException {
        if (!FAILED.equals(rsp.get(OUTCOME).asString())) {
            Assert.fail("Should have failed!");
        }
        return rsp;
    }

    private void checkResult(ModelNode result) {
        Assert.assertEquals(result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString(), "success", result.get(ModelDescriptionConstants.OUTCOME).asString());
        if (result.hasDefined(ModelDescriptionConstants.RESPONSE_HEADERS)) {
            boolean reload = result.get(ModelDescriptionConstants.RESPONSE_HEADERS, ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD).asBoolean(false);
            Assert.assertFalse("Operation should not return requires reload", reload);
        }
    }
}
