/*
* JBoss, Home of Professional Open Source.
* Copyright 2015, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.web.test;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.domain.management.security.KeystoreAttributes;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.web.WebExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.io.IOExtension;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

import java.io.File;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Stuart Douglas
 */
public class WebMigrateTestCase extends AbstractSubsystemTest {

    public static final String UNDERTOW_SUBSYSTEM_NAME = "undertow";

    public WebMigrateTestCase() {
        super(WebExtension.SUBSYSTEM_NAME, new WebExtension());
    }

    @Test
    public void testMigrateOperation() throws Exception {
        String subsystemXml = readResource("subsystem-migrate-2.2.0.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, UNDERTOW_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME);

        checkOutcome(services.executeOperation(migrateOp));

        model = services.readWholeModel();

        assertTrue(additionalInitialization.extensionAdded);
        assertFalse(model.get(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME).isDefined());
        assertTrue(model.get(SUBSYSTEM, UNDERTOW_SUBSYSTEM_NAME).isDefined());

        //make sure we have an IO subsystem
        ModelNode ioSubsystem = model.get(SUBSYSTEM, "io");
        assertTrue(ioSubsystem.isDefined());
        assertTrue(ioSubsystem.get("worker", "default").isDefined());
        assertTrue(ioSubsystem.get("buffer-pool", "default").isDefined());

        ModelNode newSubsystem = model.get(SUBSYSTEM, UNDERTOW_SUBSYSTEM_NAME);
        ModelNode newServer = newSubsystem.get("server", "default");
        assertNotNull(newServer);
        assertTrue(newServer.isDefined());
        assertEquals("default-host", newServer.get(Constants.DEFAULT_HOST).asString());


        //servlet container
        ModelNode servletContainer = newSubsystem.get(Constants.SERVLET_CONTAINER, "default");
        assertNotNull(servletContainer);
        assertTrue(servletContainer.isDefined());
        assertEquals("${prop.default-session-timeout:30}", servletContainer.get(Constants.DEFAULT_SESSION_TIMEOUT).asString());
        assertEquals("${prop.listings:true}", servletContainer.get(Constants.DIRECTORY_LISTING).asString());

        //jsp settings
        ModelNode jsp = servletContainer.get(Constants.SETTING, Constants.JSP);
        assertNotNull(jsp);
        assertEquals("${prop.recompile-on-fail:true}", jsp.get(Constants.RECOMPILE_ON_FAIL).asString());

        //welcome file
        ModelNode welcome = servletContainer.get(Constants.WELCOME_FILE, "toto");
        assertTrue(welcome.isDefined());

        //mime mapping
        ModelNode mimeMapping = servletContainer.get(Constants.MIME_MAPPING, "ogx");
        assertTrue(mimeMapping.isDefined());
        assertEquals("application/ogg", mimeMapping.get(Constants.VALUE).asString());

        //http connector
        ModelNode connector = newServer.get(Constants.HTTP_LISTENER, "http");
        assertTrue(connector.isDefined());
        assertEquals("http", connector.get(Constants.SOCKET_BINDING).asString());
        assertEquals("${prop.enabled:true}", connector.get(Constants.ENABLED).asString());
        assertEquals("${prop.enable-lookups:false}", connector.get(Constants.RESOLVE_PEER_ADDRESS).asString());
        assertEquals("${prop.max-post-size:2097153}", connector.get(Constants.MAX_POST_SIZE).asString());
        assertEquals("https", connector.get(Constants.REDIRECT_SOCKET).asString());

        //https connector
        ModelNode httpsConnector = newServer.get(Constants.HTTPS_LISTENER, "https");
        String realmName = httpsConnector.get(Constants.SECURITY_REALM).asString();
        assertTrue(realmName, realmName.startsWith("jbossweb-migration-security-realm"));
        assertEquals("${prop.session-cache-size:512}", httpsConnector.get(Constants.SSL_SESSION_CACHE_SIZE).asString());

        //realm name is dynamic
        ModelNode realm = model.get(CORE_SERVICE, MANAGEMENT).get(SECURITY_REALM, realmName);

        //trust store
        ModelNode trustStore = realm.get(AUTHENTICATION, TRUSTSTORE);
        assertEquals("${file-base}/jsse.keystore", trustStore.get(KeystoreAttributes.KEYSTORE_PATH.getName()).asString());


        //virtual host
        ModelNode virtualHost = newServer.get(Constants.HOST, "default-host");
        //welcome content
        assertEquals("welcome-content", virtualHost.get("location", "/").get(Constants.HANDLER).asString());

        assertEquals("localhost", virtualHost.get("alias").asList().get(0).asString());

        ModelNode accessLog = virtualHost.get(Constants.SETTING, Constants.ACCESS_LOG);

        assertEquals("prefix", accessLog.get(Constants.PREFIX).asString());
        assertEquals("true", accessLog.get(Constants.ROTATE).asString());
        assertEquals("extended", accessLog.get(Constants.PATTERN).asString());

        //sso
        ModelNode sso = virtualHost.get(Constants.SETTING, Constants.SINGLE_SIGN_ON);
        assertEquals("${prop.domain:myDomain}", sso.get(Constants.DOMAIN).asString());
        assertEquals("${prop.http-only:true}", sso.get(Constants.HTTP_ONLY).asString());


    }

    private static class NewSubsystemAdditionalInitialization extends AdditionalInitialization {

        UndertowExtension undertow = new UndertowExtension();
        IOExtension io = new IOExtension();

        boolean extensionAdded = false;

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            rootRegistration.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement(EXTENSION),
                    ControllerResolver.getResolver(EXTENSION), new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if(!extensionAdded) {
                        extensionAdded = true;
                        undertow.initialize(extensionRegistry.getExtensionContext("org.wildfly.extension.undertow",
                                rootRegistration, ExtensionRegistryType.SERVER));
                        io.initialize(extensionRegistry.getExtensionContext("org.wildfly.extension.io",
                                rootRegistration, ExtensionRegistryType.SERVER));
                    }
                }
            }, null));
            rootRegistration.registerSubModel(CoreManagementResourceDefinition.forStandaloneServer(new DelegatingConfigurableAuthorizer(), null, null, new EnvironmentNameReader() {
                public boolean isServer() {
                    return true;
                }

                public String getServerName() {
                    return "Test";
                }

                public String getHostName() {
                    return null;
                }

                public String getProductName() {
                    return null;
                }
            }, null));
            rootResource.registerChild(CoreManagementResourceDefinition.PATH_ELEMENT, Resource.Factory.create());
            System.setProperty("file-base", new File(getClass().getClassLoader().getResource("server.keystore").getFile()).getParentFile().getAbsolutePath());
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }
    }
}
