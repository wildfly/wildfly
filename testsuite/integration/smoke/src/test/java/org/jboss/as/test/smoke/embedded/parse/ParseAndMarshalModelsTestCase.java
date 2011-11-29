/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.smoke.embedded.parse;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CPU_AFFINITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTBOUND_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRIORITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.management.ObjectName;
import javax.xml.namespace.QName;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.ExtensionAddHandler;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.common.JVMHandlers;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyValueWriteAttributeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.management.operations.ConnectionAddHandler;
import org.jboss.as.domain.management.operations.SecurityRealmAddHandler;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.parsing.DomainXml;
import org.jboss.as.host.controller.parsing.HostXml;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.server.ServerControllerModelUtil;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the ability to parse the config files we ship or have shipped in the past, as well as the ability
 * to marshal them back to xml in a manner such that reparsing them produces a consistent in-memory configuration model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class ParseAndMarshalModelsTestCase {

    private static final String JBOSSAS_PROJECT_DIR = System.getProperty("jbossas.project.dir"); // TODO: Remove default when props start working!

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrapUtils.createJavaArchive("bogus.jar", ParseAndMarshalModelsTestCase.class.getPackage())
                .add(new Asset() {
                    public InputStream openStream() {
                        return new ByteArrayInputStream("Dependencies: org.jboss.staxmapper,org.jboss.as.controller,org.jboss.as.deployment-repository,org.jboss.as.server,org.jboss.as.host-controller,org.jboss.as.domain-management\n\n".getBytes());
                    }
                 }, "META-INF/MANIFEST.MF");
    }

    private ServiceContainer serviceContainer;

    @Before
    public void setupServiceContainer() {
        serviceContainer = ServiceContainer.Factory.create("test");
    }

    @After
    public void cleanup() throws Exception {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName("jboss.msc:type=container,name=test"));
    }

    @Test
    public void testStandaloneXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone.xml"));
    }

    @Test
    public void testStandaloneHAXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-ha.xml"));
    }

    @Test
    public void testStandaloneOSGiOnlyXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-osgi-only.xml"));
    }

    @Test
    public void testStandaloneXtsXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-xts.xml"));
    }

    @Test
    public void test700StandaloneXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-0.xml"));
    }

    @Test
    public void test701StandaloneXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-1.xml"));
    }

    @Test
    public void test702StandaloneXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-2.xml"));
    }

    @Test
    public void test700StandaloneHAXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-0-ha.xml"));
    }

    @Test
    public void test701StandaloneHAXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-1-ha.xml"));
    }

    @Test
    public void test702StandaloneHAXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-2-ha.xml"));
    }

    @Test
    public void test700StandalonePreviewXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-0-preview.xml"));
    }

    @Test
    public void test701StandalonePreviewXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-1-preview.xml"));
    }

    @Test
    public void test702StandalonePreviewXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-2-preview.xml"));
    }

    @Test
    public void test700StandalonePreviewHAXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-0-preview.xml"));
    }

    @Test
    public void test701StandalonePreviewHAXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-1-preview-ha.xml"));
    }

    @Test
    public void test702StandalonePreviewHAXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-2-preview-ha.xml"));
    }

    @Test
    public void test701StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-1-xts.xml"));
    }

    @Test
    public void test702StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-0-2-xts.xml"));
    }

    private void standaloneXmlTest(File original) throws Exception {

        File file = new File("target/standalone-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(original, file);
        ModelNode originalModel = loadServerModel(file);
        ModelNode reparsedModel = loadServerModel(file);

        fixupOSGiStandalone(originalModel, reparsedModel);

        compare(originalModel, reparsedModel);
    }

    @Test
    public void testHostXml() throws Exception {
        hostXmlTest(getOriginalHostXml("host.xml"));
    }

    @Test
    public void test700HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-0-0.xml"));
    }

    @Test
    public void test701HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-0-1.xml"));
    }

    @Test
    public void test702HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-0-2.xml"));
    }

    private void hostXmlTest(final File original) throws Exception {
        File file = new File("target/host-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(original, file);
        ModelNode originalModel = loadHostModel(file);
        ModelNode reparsedModel = loadHostModel(file);

        compare(originalModel, reparsedModel);
    }

    @Test
    public void testDomainXml() throws Exception {
        domainXmlTest(getOriginalDomainXml("domain.xml"));
    }

    @Test
    public void testDomainOSGiOnlyXml() throws Exception {
        domainXmlTest(getOriginalDomainXml("domain-osgi-only.xml"));
    }

    @Test
    public void test700DomainXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-0-0.xml"));
    }

    @Test
    public void test701DomainXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-0-1.xml"));
    }

    @Test
    public void test702DomainXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-0-2.xml"));
    }

    @Test
    public void test700DomainPreviewXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-0-0-preview.xml"));
    }

    @Test
    public void test701DomainPreviewXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-0-1-preview.xml"));
    }

    @Test
    public void test702DomainPreviewXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-0-2-preview.xml"));
    }


    private void domainXmlTest(File original) throws Exception {
        File file = new File("target/domain-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(original, file);
        ModelNode originalModel = loadDomainModel(file);
        ModelNode reparsedModel = loadDomainModel(file);

        fixupOSGiDomain(originalModel, reparsedModel);
        compare(originalModel, reparsedModel);
    }

    private void fixupOSGiStandalone(ModelNode node1, ModelNode node2) {
        //These multiline properties get extra indentation when marshalled. Put them on one line to compare properly
        node1.get("subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node1.get("subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node2.get("subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node2.get("subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node1.get("subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node1.get("subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));
        node2.get("subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node2.get("subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));
    }

    private void fixupOSGiDomain(ModelNode node1, ModelNode node2) {
        //These multiline properties get extra indentation when marshalled. Put them on one line to compare properly
        node1.get("profile", "default", "subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node1.get("profile", "default", "subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node2.get("profile", "default", "subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node2.get("profile", "default", "subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node1.get("profile", "default", "subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node1.get("profile", "default", "subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));
        node2.get("profile", "default", "subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node2.get("profile", "default", "subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));

        node1.get("profile", "ha", "subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node1.get("profile", "ha", "subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node2.get("profile", "ha", "subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node2.get("profile", "ha", "subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node1.get("profile", "ha", "subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node1.get("profile", "ha", "subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));
        node2.get("profile", "ha", "subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node2.get("profile", "ha", "subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));
    }

    private String convertToSingleLine(String value) {
        //Reformat the string so it works better in ParseAndMarshalModelsTestCase
        String[] values = value.split(",");
        StringBuilder formattedValue = new StringBuilder();
        boolean first = true;
        for (String val : values) {
            val = val.trim();
            if (!first) {
                formattedValue.append(", ");
            } else {
                first = false;
            }
            formattedValue.append(val);
        }
        return formattedValue.toString();
    }

    private void compare(ModelNode node1, ModelNode node2) {
        Assert.assertEquals(node1.getType(), node2.getType());
        if (node1.getType() == ModelType.OBJECT) {
            final Set<String> keys1 = node1.keys();
            final Set<String> keys2 = node2.keys();
            Assert.assertEquals(node1 + "\n" + node2, keys1.size(), keys2.size());

            for (String key : keys1) {
                final ModelNode child1 = node1.get(key);
                Assert.assertTrue("Missing: " + key + "\n" + node1 + "\n" + node2, node2.has(key));
                final ModelNode child2 = node2.get(key);
                if (child1.isDefined()) {
                    Assert.assertTrue(child1.toString(), child2.isDefined());
                    compare(child1, child2);
                } else {
                    Assert.assertFalse(child2.asString(), child2.isDefined());
                }
            }
        } else if (node1.getType() == ModelType.LIST) {
            List<ModelNode> list1 = node1.asList();
            List<ModelNode> list2 = node2.asList();
            Assert.assertEquals(list1 + "\n" + list2, list1.size(), list2.size());

            for (int i = 0; i < list1.size(); i++) {
                compare(list1.get(i), list2.get(i));
            }

        } else if (node1.getType() == ModelType.PROPERTY) {
            Property prop1 = node1.asProperty();
            Property prop2 = node2.asProperty();
            Assert.assertEquals(prop1 + "\n" + prop2, prop1.getName(), prop2.getName());
            compare(prop1.getValue(), prop2.getValue());

        } else {
            try {
                Assert.assertEquals("\n\"" + node1.asString() + "\"\n\"" + node2.asString() + "\"\n-----", node1.asString().trim(), node2.asString().trim());
            } catch (AssertionFailedError error) {
                throw error;
            }
        }
    }

    private ModelNode loadServerModel(final File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
        final StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader(), null);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.values()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "server"), parser);
            }
        }
        final List<ModelNode> ops = persister.load();

        final ModelNode model = new ModelNode();
        final ModelController controller = createController(model, new Setup() {
            public void setup(Resource resource, ManagementResourceRegistration rootRegistration) {
                ServerControllerModelUtil.updateCoreModel(model);
                ServerControllerModelUtil.initOperations(rootRegistration, null, persister, null, null, null, false);
            }
        });

        final ModelNode caputreModelOp = new ModelNode();
        caputreModelOp.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        caputreModelOp.get(OP).set("capture-model");

        final List<ModelNode> toRun = new ArrayList<ModelNode>(ops);
        toRun.add(caputreModelOp);
        executeOperations(controller, toRun);
        persister.store(model, null).commit();
        return model;
    }

    private ModelNode loadHostModel(final File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "host");
        final HostXml parser = new HostXml(Module.getBootModuleLoader(), null);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.values()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "host"), parser);
            }
        }
        final List<ModelNode> ops = persister.load();

        final ModelNode model = new ModelNode();

        final ModelController controller = createController(model, new Setup() {
            public void setup(Resource resource, ManagementResourceRegistration root) {

                final Resource host = Resource.Factory.create();
                resource.registerChild(PathElement.pathElement(HOST, "master"), host);

                // TODO maybe make creating of empty nodes part of the MNR description
                host.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
                host.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());

                final LocalHostControllerInfoImpl hostControllerInfo = new LocalHostControllerInfoImpl(new ControlledProcessState(false));

                // Add of the host itself
                ManagementResourceRegistration hostRegistration = root.registerSubModel(PathElement.pathElement(HOST), HostDescriptionProviders.HOST_ROOT_PROVIDER);

                // Other root resource operations
                XmlMarshallingHandler xmh = new XmlMarshallingHandler(persister);
                hostRegistration.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, OperationEntry.EntryType.PUBLIC);
                hostRegistration.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
                hostRegistration.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
                hostRegistration.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), AttributeAccess.Storage.CONFIGURATION);
                hostRegistration.registerReadOnlyAttribute(MASTER, IsMasterHandler.INSTANCE, AttributeAccess.Storage.RUNTIME);

                // System Properties
                ManagementResourceRegistration sysProps = hostRegistration.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SYSTEM_PROPERTIES_PROVIDER);
                sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);

                // Central Management
                ManagementResourceRegistration management = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), CommonProviders.MANAGEMENT_WITH_INTERFACES_PROVIDER);
                ManagementResourceRegistration securityRealm = management.registerSubModel(PathElement.pathElement(SECURITY_REALM), CommonProviders.MANAGEMENT_SECURITY_REALM_PROVIDER);
                securityRealm.registerOperationHandler(SecurityRealmAddHandler.OPERATION_NAME, SecurityRealmAddHandler.INSTANCE, SecurityRealmAddHandler.INSTANCE, false);

                ManagementResourceRegistration connection = management.registerSubModel(PathElement.pathElement(OUTBOUND_CONNECTION), CommonProviders.MANAGEMENT_OUTBOUND_CONNECTION_PROVIDER);
                connection.registerOperationHandler(ConnectionAddHandler.OPERATION_NAME, ConnectionAddHandler.INSTANCE, ConnectionAddHandler.INSTANCE, false);
                // Management API protocols
                management.registerSubModel(new NativeManagementResourceDefinition(hostControllerInfo));
                management.registerSubModel(new HttpManagementResourceDefinition(hostControllerInfo, null));

                //Extensions
                ManagementResourceRegistration extensions = hostRegistration.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
                ExtensionContext extensionContext = new ExtensionContextImpl(hostRegistration, null, persister, ExtensionContext.ProcessType.STANDALONE_SERVER);
                ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext, false);
                extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);

                // Jvms
                final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
                JVMHandlers.register(jvms);

                //Paths
                ManagementResourceRegistration paths = hostRegistration.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
                paths.registerOperationHandler(PathAddHandler.OPERATION_NAME, PathAddHandler.SPECIFIED_INSTANCE, PathAddHandler.SPECIFIED_INSTANCE, false);

                //interface
                ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
                HostSpecifiedInterfaceAddHandler hsiah = new HostSpecifiedInterfaceAddHandler(hostControllerInfo);
                interfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, hsiah, hsiah, false);
                InterfaceCriteriaWriteHandler.register(interfaces);

                //server
                ManagementResourceRegistration servers = hostRegistration.registerSubModel(PathElement.pathElement(SERVER_CONFIG), HostDescriptionProviders.SERVER_PROVIDER);
                servers.registerOperationHandler(ServerAddHandler.OPERATION_NAME, ServerAddHandler.INSTANCE, ServerAddHandler.INSTANCE, false);
                servers.registerReadWriteAttribute(AUTO_START, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), AttributeAccess.Storage.CONFIGURATION);
                servers.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
                servers.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0), AttributeAccess.Storage.CONFIGURATION);
                servers.registerReadWriteAttribute(PRIORITY, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0), AttributeAccess.Storage.CONFIGURATION);
                servers.registerReadWriteAttribute(CPU_AFFINITY, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), AttributeAccess.Storage.CONFIGURATION);


                //server paths
                ManagementResourceRegistration serverPaths = servers.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
                serverPaths.registerOperationHandler(PathAddHandler.OPERATION_NAME, PathAddHandler.SPECIFIED_INSTANCE, PathAddHandler.SPECIFIED_INSTANCE, false);
                //server interfaces
                ManagementResourceRegistration serverInterfaces = servers.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
                serverInterfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
                InterfaceCriteriaWriteHandler.register(serverInterfaces);

                // Server system Properties
                ManagementResourceRegistration serverSysProps = servers.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), HostDescriptionProviders.SERVER_SYSTEM_PROPERTIES_PROVIDER);
                serverSysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
                serverSysProps.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
                serverSysProps.registerReadWriteAttribute(BOOT_TIME, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), AttributeAccess.Storage.CONFIGURATION);

                // Server jvm
                final ManagementResourceRegistration serverVMs = servers.registerSubModel(PathElement.pathElement(JVM), JVMHandlers.SERVER_MODEL_PROVIDER);
                JVMHandlers.register(serverVMs, true);
            }
        });

        final ModelNode caputreModelOp = new ModelNode();
        caputreModelOp.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        caputreModelOp.get(OP).set("capture-model");

        final List<ModelNode> toRun = new ArrayList<ModelNode>(ops);
        toRun.add(caputreModelOp);
        executeOperations(controller, toRun);

        model.get(HOST, "master", NAME).set("master");
        persister.store(model.get(HOST, "master"), null).commit();
        return model;
    }

    private ModelNode loadDomainModel(File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "domain");
        final DomainXml parser = new DomainXml(Module.getBootModuleLoader(), null);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.values()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "domain"), parser);
            }
        }
        final List<ModelNode> ops = persister.load();


        final ModelNode model = new ModelNode();
        final ModelController controller = createController(model, new Setup() {
            public void setup(Resource resource, ManagementResourceRegistration rootRegistration) {
                DomainModelUtil.updateCoreModel(resource.getModel());
                DomainModelUtil.initializeMasterDomainRegistry(rootRegistration, persister, null, new MockFileRepository(), new MockDomainController(), null);
            }
        });

        final ModelNode caputreModelOp = new ModelNode();
        caputreModelOp.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        caputreModelOp.get(OP).set("capture-model");

        final List<ModelNode> toRun = new ArrayList<ModelNode>(ops);
        toRun.add(caputreModelOp);

        executeOperations(controller, toRun);

        //
        persister.store(model, null).commit();
        return model;
    }

    public ModelController createController(final ModelNode model, final Setup registration) throws InterruptedException {
        final ServiceController<?> existingController = serviceContainer.getService(ServiceName.of("ModelController"));
        if (existingController != null) {
            final CountDownLatch latch = new CountDownLatch(1);
            existingController.addListener(new AbstractServiceListener<Object>() {
                public void listenerAdded(ServiceController<?> serviceController) {
                    serviceController.setMode(ServiceController.Mode.REMOVE);
                }

                public void transition(ServiceController<?> serviceController, ServiceController.Transition transition) {
                    if (transition.equals(ServiceController.Transition.REMOVING_to_REMOVED)) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        }

        ServiceTarget target = serviceContainer.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        ModelControllerService svc = new ModelControllerService(processState, registration, model);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await();
        ModelController controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
        processState.setRunning();
        return controller;
    }

    private void executeOperations(ModelController controller, List<ModelNode> ops) {
        for (final ModelNode op : ops) {
            controller.execute(op, null, null, null);
        }
    }

    private File getOriginalStandaloneXml(String profile) {
				File f = new File( System.getProperty("jbossas.project.dir", "../../..") );
        f = f.getAbsoluteFile();
        Assert.assertTrue(f.exists());
        f = new File(f, "build");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "src");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "main");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "resources");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "standalone");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "configuration");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getDomainConfigDir() {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
				File f = new File( System.getProperty("jbossas.project.dir", "../../..") );
        f = f.getAbsoluteFile();
        Assert.assertTrue(f.exists());
        f = new File(f, "build");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "src");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "main");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "resources");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "domain");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "configuration");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getOriginalHostXml(final String profile) {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = getDomainConfigDir();
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getOriginalDomainXml(final String profile) {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = getDomainConfigDir();
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getLegacyConfigFile(String type, final String profile) {
				File f = new File( System.getProperty("jbossas.ts.submodule.dir") );
        Assert.assertTrue(f.exists());
        f = new File(f, "src");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "test");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "resources");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "legacy-configs");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, type);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;

    }

    private void copyFile(final File src, final File dest) throws Exception {
        final InputStream in = new BufferedInputStream(new FileInputStream(src));
        try {
            dest.getParentFile().mkdirs();
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            try {
                int i = in.read();
                while (i != -1) {
                    out.write(i);
                    i = in.read();
                }
            } finally {
                close(out);
            }
        } finally {
            close(in);
        }
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }

    DescriptionProvider getRootDescriptionProvider() {
        return new DescriptionProvider() {
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        };
    }

    interface Setup {
        void setup(Resource resource, ManagementResourceRegistration rootRegistration);
    }

    class ModelControllerService extends AbstractControllerService {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final ModelNode model;
        private final Setup registration;

        ModelControllerService(final ControlledProcessState processState, final Setup registration, final ModelNode model) {
            super(OperationContext.Type.MANAGEMENT, new NullConfigurationPersister(), processState, getRootDescriptionProvider(), null, ExpressionResolver.DEFAULT);
            this.model = model;
            this.registration = registration;
        }

        @Override
        protected void boot(BootContext context) throws ConfigurationPersistenceException {
            try {
                super.boot(context);
            } finally {
                latch.countDown();
            }
        }

        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            registration.setup(rootResource, rootRegistration);

            rootRegistration.registerOperationHandler("capture-model", new OperationStepHandler() {
                        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                            model.set(Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
                        }
                    }, getRootDescriptionProvider());
            // TODO maybe make creating of empty nodes part of the MNR description
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());
        }


    }

    private static class MockContentRepository implements ContentRepository {

        private static final MockContentRepository INSTANCE = new MockContentRepository();

        @Override
        public byte[] addContent(InputStream stream) throws IOException {
            return null;
        }

        @Override
        public VirtualFile getContent(byte[] hash) {
            throw new RuntimeException("NYI: org.jboss.as.test.surefire.xml.ParseAndMarshalModelsTestCase.MockContentRepository.getContent");
        }

        @Override
        public boolean hasContent(byte[] hash) {
            return true;
        }

        @Override
        public void removeContent(byte[] hash) {
            throw new RuntimeException("NYI: org.jboss.as.test.surefire.xml.ParseAndMarshalModelsTestCase.MockContentRepository.removeContent");
        }

    }

    private static class MockFileRepository implements FileRepository {

        private static final MockFileRepository INSTANCE = new MockFileRepository();

        @Override
        public File getFile(String relativePath) {
            return null;
        }

        @Override
        public File getConfigurationFile(String relativePath) {
            return null;
        }

        @Override
        public File[] getDeploymentFiles(byte[] deploymentHash) {
            return null;
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            return null;
        }
    }

    private static class MockDomainController implements DomainController {
        public LocalHostControllerInfo getLocalHostInfo() {
            return null;
        }

        public void registerRemoteHost(ProxyController hostControllerClient) {
        }

        public void unregisterRemoteHost(String id) {
        }

        public void registerRunningServer(ProxyController serverControllerClient) {
        }

        public void unregisterRunningServer(String serverName) {
        }

        public ModelNode getProfileOperations(String profileName) {
            return null;
        }

        public FileRepository getLocalFileRepository() {
            return null;
        }

        public FileRepository getRemoteFileRepository() {
            return null;
        }

        public void stopLocalHost() {
        }
    }
}
