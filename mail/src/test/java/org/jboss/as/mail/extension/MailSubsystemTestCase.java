/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.security.credential.store.CredentialStore;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
@RunWith(Parameterized.class)
public class MailSubsystemTestCase extends AbstractSubsystemBaseTest {
    // TODO Create formal enumeration of schema versions
    @Parameters
    public static Collection<Object[]> parameters() {
        return List.of(
                new Object[] { 1, 0 },
                new Object[] { 1, 1 },
                new Object[] { 1, 2 },
                new Object[] { 2, 0 },
                new Object[] { 3, 0 },
                new Object[] { 4, 0 });
    }

    private final Map<ServiceName, Supplier<Object>> values = new ConcurrentHashMap<>();
    private final int major;
    private final int minor;

    public MailSubsystemTestCase(int major, int minor) {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
        this.major = major;
        this.minor = minor;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(String.format("subsystem_%d_%d.xml", this.major, this.minor));
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format("schema/%s-mail_%d_%d.xsd", (this.major == 1) ? "jboss-as" : "wildfly", this.major, this.minor);
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    /**
     * Tests that runtime information is the expected one based on the subsystem_4_0.xml subsystem configuration.
     *
     * @throws Exception
     */
    @Test
    public void testRuntime() throws Exception {
        if (this.major >= 2) {
            KernelServices services = createKernelServicesBuilder(new DefaultInitializer(this.values)).setSubsystemXml(getSubsystemXml()).build();
            if (!services.isSuccessfulBoot()) {
                Assert.fail(services.getBootError().toString());
            }

            SessionProvider provider = (SessionProvider) this.values.get(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("defaultMail").append("provider")).get();
            Assert.assertNotNull("session should not be null", provider);
            Session session = provider.getSession();
            Properties properties = session.getProperties();
            Assert.assertNotNull("smtp host should be set", properties.getProperty("mail.smtp.host"));
            Assert.assertNotNull("pop3 host should be set", properties.getProperty("mail.pop3.host"));
            Assert.assertNotNull("imap host should be set", properties.getProperty("mail.imap.host"));

            if (this.major >= 3) {
                PasswordAuthentication auth = session.requestPasswordAuthentication(InetAddress.getLocalHost(), 25, "smtp", "", "");
                Assert.assertEquals("nobody", auth.getUserName());
                Assert.assertEquals("pass", auth.getPassword());
            }

            provider = (SessionProvider) this.values.get(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("default2").append("provider")).get();
            session = provider.getSession();
            Assert.assertEquals("Debug should be true", true, session.getDebug());

            provider = (SessionProvider) this.values.get(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("custom").append("provider")).get();
            session = provider.getSession();
            properties = session.getProperties();
            String host = properties.getProperty("mail.smtp.host");
            Assert.assertNotNull("smtp host should be set", host);
            Assert.assertEquals("mail.example.com", host);

            Assert.assertEquals("localhost", properties.get("mail.pop3.host")); //this one should be read out of socket binding
            Assert.assertEquals("some-custom-prop-value", properties.get("mail.pop3.custom_prop")); //this one should be extra property
            Assert.assertEquals("fully-qualified-prop-name", properties.get("some.fully.qualified.property")); //this one should be extra property
        }
    }

    /**
     * Tests that runtime information coming from attribute expressions is the expected one based on the subsystem_4_0.xml subsystem configuration.
     *
     * @throws Exception
     */
    @Test
    public void testExpressionsRuntime() throws Exception {
        if (this.major >= 4) {
            KernelServices services = createKernelServicesBuilder(new DefaultInitializer(this.values)).setSubsystemXml(getSubsystemXml()).build();
            if (!services.isSuccessfulBoot()) {
                Assert.fail(services.getBootError().toString());
            }

            ConfigurableSessionProvider provider = (ConfigurableSessionProvider) this.values.get(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("default3").append("provider")).get();
            MailSessionConfig config = provider.getConfig();

            Assert.assertEquals("Unexpected value for mail-session=default3 from attribute", "from@from.org", config.getFrom());
            Assert.assertEquals("Unexpected value for mail-session=default3 jndi-name attribute", "java:jboss/mail/Default3", config.getJndiName());
            Assert.assertEquals("Unexpected value for mail-session=default3 debug attribute", Boolean.TRUE, config.isDebug());

            ServerConfig smtpServerConfig = config.getSmtpServer();
            Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/tls attribute", Boolean.TRUE, smtpServerConfig.isTlsEnabled());
            Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/ssl attribute", Boolean.FALSE, smtpServerConfig.isSslEnabled());

            Credentials credentials = smtpServerConfig.getCredentials();
            Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/username attribute", "nobody", credentials.getUsername());
            Assert.assertEquals("Unexpected value for mail-session=default3 smtp-server/password attribute", "empty", credentials.getPassword());

            provider = (ConfigurableSessionProvider) this.values.get(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("custom3").append("provider")).get();
            config = provider.getConfig();
            CustomServerConfig customServerConfig = config.getCustomServers()[0];
            Map<String, String> properties = customServerConfig.getProperties();
            Assert.assertEquals("Unexpected value for mail-session=custom3 custom-server/property value attribute", "mail.example.com", properties.get("host"));
        }
    }

    @Test
    public void testOperations() throws Exception {
        if (this.major >= 2) {
            KernelServices services = createKernelServicesBuilder(new DefaultInitializer(this.values)).setSubsystemXml(getSubsystemXml()).build();
            if (!services.isSuccessfulBoot()) {
                Assert.fail(services.getBootError().toString());
            }

            PathAddress sessionAddress = PathAddress.pathAddress(MailExtension.SUBSYSTEM_PATH, PathElement.pathElement(MailExtension.MAIL_SESSION_PATH.getKey(), "defaultMail"));
            ModelNode result;

            ModelNode removeServerOp = Util.createRemoveOperation(sessionAddress.append("server", "imap"));
            removeServerOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            result = services.executeOperation(removeServerOp);
            checkResult(result);

            ModelNode addServerOp = Util.createAddOperation(sessionAddress.append("server", "imap"));
            addServerOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            addServerOp.get("outbound-socket-binding-ref").set("mail-imap");
            addServerOp.get("username").set("user");
            addServerOp.get("password").set("pswd");

            result = services.executeOperation(addServerOp);
            checkResult(result);

            checkResult(services.executeOperation(removeServerOp)); //to make sure noting is left behind
            checkResult(services.executeOperation(addServerOp));

            ModelNode writeOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, sessionAddress);
            writeOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            writeOp.get("name").set("debug");
            writeOp.get("value").set(false);
            result = services.executeOperation(writeOp);
            checkResult(result);

            SessionProvider provider = (SessionProvider) this.values.get(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("defaultMail").append("provider")).get();
            Session session = provider.getSession();
            Assert.assertNotNull("session should not be null", session);
            Properties properties = session.getProperties();
            Assert.assertNotNull("smtp host should be set", properties.getProperty("mail.smtp.host"));
            Assert.assertNotNull("imap host should be set", properties.getProperty("mail.imap.host"));

            PathAddress nonExisting = PathAddress.pathAddress(MailExtension.SUBSYSTEM_PATH, PathElement.pathElement(MailExtension.MAIL_SESSION_PATH.getKey(), "non-existing-session"));
            ModelNode addSession = Util.createAddOperation(nonExisting);
            addSession.get("jndi-name").set("java:/bah");
            checkResult(services.executeOperation(addSession));
            removeServerOp = Util.createRemoveOperation(nonExisting.append("server", "imap"));
            removeServerOp.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            result = services.executeOperation(removeServerOp);
            checkForFailure(result);
        }
    }

    private static ModelNode checkForFailure(ModelNode rsp) {
        if (!FAILED.equals(rsp.get(OUTCOME).asString())) {
            Assert.fail("Should have failed!");
        }
        return rsp;
    }

    private static void checkResult(ModelNode result) {
        Assert.assertEquals(result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString(), ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());
        if (result.hasDefined(ModelDescriptionConstants.RESPONSE_HEADERS)) {
            boolean reload = result.get(ModelDescriptionConstants.RESPONSE_HEADERS, ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD).asBoolean(false);
            Assert.assertFalse("Operation should not return requires reload", reload);
        }
    }

    public static class DefaultInitializer extends AdditionalInitialization {
        private final Map<String, Integer> sockets = new HashMap<>();
        private final Map<ServiceName, Supplier<Object>> values;

        public DefaultInitializer(Map<ServiceName, Supplier<Object>> values) {
            this.values = values;
            sockets.put("mail-imap", 432);
            sockets.put("mail-pop3", 1234);
            sockets.put("mail-smtp", 25);
        }

        private void record(ServiceTarget target, ServiceName name) {
            ServiceBuilder<?> builder = target.addService(name.append("test-recorder"));
            this.values.put(name, builder.requires(name));
            builder.setInstance(Service.NULL).setInitialMode(ServiceController.Mode.ACTIVE).install();
        }

        @Override
        protected void setupController(ControllerInitializer controllerInitializer) {
            super.setupController(controllerInitializer);

            for (Map.Entry<String, Integer> entry : sockets.entrySet()) {
                controllerInitializer.addRemoteOutboundSocketBinding(entry.getKey(), "localhost", entry.getValue());

            }
            //bug in framework, it doesn't work if only outbound socket bindings are present
            controllerInitializer.addSocketBinding("useless", 9999);
        }

        @Override
        protected void addExtraServices(ServiceTarget target) {

            target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new NamingStoreService())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, new NamingStoreService())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            this.record(target, MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("defaultMail").append("provider"));
            this.record(target, MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("default2").append("provider"));
            this.record(target, MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("custom").append("provider"));
            this.record(target, MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("default3").append("provider"));
            this.record(target, MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName("custom3").append("provider"));
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                                        ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            Map<String, Class> capabilities = new HashMap<>();
            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.credential-store", "my-credential-store"), CredentialStore.class);

            capabilities.put(buildDynamicCapabilityName("org.wildfly.security.ssl-context", "foo"), SSLContext.class);
            //capabilities.put(buildDynamicCapabilityName("org.wildfly.network.outbound-socket-binding","ajp-remote"), OutboundSocketBinding.class);


            registerServiceCapabilities(capabilityRegistry, capabilities);
            registerCapabilities(capabilityRegistry,
                    RuntimeCapability.Builder.of(OutboundSocketBinding.SERVICE_DESCRIPTOR).build(),
                    RuntimeCapability.Builder.of("org.wildfly.security.ssl-context", true, SSLContext.class).build()
            );


        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }
    }
}
