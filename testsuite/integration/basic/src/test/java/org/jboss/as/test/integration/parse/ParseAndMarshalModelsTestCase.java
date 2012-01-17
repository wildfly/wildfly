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
package org.jboss.as.test.integration.parse;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CPU_AFFINITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRIORITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;

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
import java.util.concurrent.TimeUnit;

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
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.extension.ExtensionRegistry;
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
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.parsing.DomainXml;
import org.jboss.as.host.controller.parsing.HostXml;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.security.vault.RuntimeVaultReader;
import org.jboss.as.server.ServerControllerModelUtil;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.as.server.file.repository.api.HostFileRepository;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.as.server.services.net.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.services.security.VaultAddHandler;
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the ability to parse the config files we ship or have shipped in the past, as well as the ability
 * to marshal them back to xml in a manner such that reparsing them produces a consistent in-memory configuration model.
 * <p>
 * <b>Note:</b>The standard {@code build/src/main/resources/standalone/configuration} and
 * {@code build/src/main/resources/domain/configuration} files are tested in the smoke integration module ParseAndMarshalModelsTestCase.
 *
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class ParseAndMarshalModelsTestCase {

    @Deployment
    public static Archive<?> getDeployment() {

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bogus.jar");
        archive.addPackage(ParseAndMarshalModelsTestCase.class.getPackage());
        archive.add(new Asset() {
                    public InputStream openStream() {
                        return new ByteArrayInputStream("Dependencies: org.jboss.staxmapper,org.jboss.as.controller,org.jboss.as.deployment-repository,org.jboss.as.server,org.jboss.as.host-controller,org.jboss.as.domain-management,org.jboss.as.security\n\n".getBytes());
                    }
                 }, "META-INF/MANIFEST.MF");
        return archive;
    }

    private ServiceContainer serviceContainer;

    @Before
    public void setupServiceContainer() {
        serviceContainer = ServiceContainer.Factory.create("test");
    }

    @After
    public void cleanup() throws Exception {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName("jboss.msc:type=container,name=test"));
        if (serviceContainer != null) {
            serviceContainer.shutdown();
            try {
                serviceContainer.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                serviceContainer = null;
            }
        }
    }

    @Test
    public void testStandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getExampleConfigFile("standalone-minimalistic.xml"));
    }

    @Test
    public void testStandaloneOSGiOnlyXml() throws Exception {
        standaloneXmlTest(getExampleConfigFile("standalone-osgi-only.xml"));
    }

    @Test
    public void testStandaloneXtsXml() throws Exception {
        standaloneXmlTest(getExampleConfigFile("standalone-xts.xml"));
    }

    @Test @Ignore("AS7-2901")
    public void testStandaloneHornetqColocatedXml() throws Exception {
        standaloneXmlTest(getExampleConfigFile("standalone-hornetq-colocated.xml"));
    }

    @Test
    public void testStandaloneJtsXml() throws Exception {
        standaloneXmlTest(getExampleConfigFile("standalone-jts.xml"));
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
    public void testDomainOSGiOnlyXml() throws Exception {
        domainXmlTest(getExampleConfigFile("domain-osgi-only.xml"));
    }

    @Test
    public void testDomainJtsXml() throws Exception {
        domainXmlTest(getExampleConfigFile("domain-jts.xml"));
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
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.NORMAL));
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
        final StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader(), null, extensionRegistry);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.values()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "server"), parser);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        final List<ModelNode> ops = persister.load();

        final ModelNode model = new ModelNode();
        final ModelController controller = createController(ProcessType.STANDALONE_SERVER, model, new Setup() {
            public void setup(Resource resource, ManagementResourceRegistration rootRegistration) {
                ServerControllerModelUtil.updateCoreModel(model, null);
                ServerControllerModelUtil.initOperations(rootRegistration, new MockContentRepository(), persister, null, null, null, null, extensionRegistry, false, new MockFileRepository());
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

//        System.out.println(ops);

        final ModelNode model = new ModelNode();

        final ModelController controller = createController(ProcessType.HOST_CONTROLLER, model, new Setup() {
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

                //vault
                ManagementResourceRegistration vault = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, VAULT), CommonProviders.VAULT_PROVIDER);
                VaultAddHandler vah = new VaultAddHandler(new MockVaultReader());
                vault.registerOperationHandler(VaultAddHandler.OPERATION_NAME, vah, vah, false);

                // Central Management
                ManagementResourceRegistration management = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), CommonProviders.MANAGEMENT_WITH_INTERFACES_PROVIDER);
                management.registerSubModel(SecurityRealmResourceDefinition.INSTANCE);
                management.registerSubModel(LdapConnectionResourceDefinition.INSTANCE);
                management.registerSubModel(new NativeManagementResourceDefinition(hostControllerInfo));
                management.registerSubModel(new HttpManagementResourceDefinition(hostControllerInfo, null));

                // Domain controller
                LocalDomainControllerAddHandler localDcAddHandler = new MockLocalDomainControllerAddHandler();
                hostRegistration.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, localDcAddHandler, localDcAddHandler, false);
                RemoteDomainControllerAddHandler remoteDcAddHandler = new MockRemoteDomainControllerAddHandler();
                hostRegistration.registerOperationHandler(RemoteDomainControllerAddHandler.OPERATION_NAME, remoteDcAddHandler, remoteDcAddHandler, false);

                // Jvms
                final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
                JVMHandlers.register(jvms);

                //Paths
                ManagementResourceRegistration paths = hostRegistration.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
                paths.registerOperationHandler(PathAddHandler.OPERATION_NAME, PathAddHandler.SPECIFIED_INSTANCE, PathAddHandler.SPECIFIED_INSTANCE, false);

                //interface
                ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
                HostSpecifiedInterfaceAddHandler hsiah = new HostSpecifiedInterfaceAddHandler();
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

//        System.out.println(model.toString());

        return model;
    }

    private ModelNode loadDomainModel(File file) throws Exception {
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.NORMAL));
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "domain");
        final DomainXml parser = new DomainXml(Module.getBootModuleLoader(), null, extensionRegistry);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.values()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "domain"), parser);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        final List<ModelNode> ops = persister.load();


        final ModelNode model = new ModelNode();
        final ModelController controller = createController(ProcessType.HOST_CONTROLLER, model, new Setup() {
            public void setup(Resource resource, ManagementResourceRegistration rootRegistration) {
                DomainModelUtil.updateCoreModel(resource, null);
                DomainModelUtil.initializeMasterDomainRegistry(rootRegistration, persister, new MockContentRepository(), new MockFileRepository(),
                        new MockDomainController(), null, extensionRegistry);
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

    public ModelController createController(final ProcessType processType, final ModelNode model, final Setup registration) throws InterruptedException {
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
        ModelControllerService svc = new ModelControllerService(processType, processState, registration, model);
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
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            controller.execute(op, null, null, null);
        }
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

    private File getExampleConfigFile(String name) {
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
        f = new File(f, "docs");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "examples");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, "configs");
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        f = new File(f, name);
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

        ModelControllerService(final ProcessType processType, final ControlledProcessState processState, final Setup registration, final ModelNode model) {
            super(processType, new RunningModeControl(RunningMode.ADMIN_ONLY), new NullConfigurationPersister(), processState, getRootDescriptionProvider(), null, ExpressionResolver.DEFAULT);
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

        @Override
        public byte[] addContent(InputStream stream) throws IOException {
            return null;
        }

        @Override
        public VirtualFile getContent(byte[] hash) {
            return null;
        }

        @Override
        public boolean hasContent(byte[] hash) {
            return false;
        }

        @Override
        public void removeContent(byte[] hash) {
        }

    }

    private static class MockFileRepository implements HostFileRepository {

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

        @Override
        public void deleteDeployment(byte[] deploymentHash) {
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

        public HostFileRepository getLocalFileRepository() {
            return null;
        }

        public HostFileRepository getRemoteFileRepository() {
            return null;
        }

        public void stopLocalHost() {
        }

        @Override
        public void stopLocalHost(int exitCode) {
            //
        }
    }

    private static class MockVaultReader extends RuntimeVaultReader {
    }

    private static class MockLocalDomainControllerAddHandler extends LocalDomainControllerAddHandler {

        /**
         * Create the ServerAddHandler
         */
        protected MockLocalDomainControllerAddHandler() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        protected void initializeDomain() {
            // no-op
        }
    }

    private static class MockRemoteDomainControllerAddHandler extends RemoteDomainControllerAddHandler {

        /**
         * Create the ServerAddHandler
         */
        protected MockRemoteDomainControllerAddHandler() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected void initializeDomain(OperationContext context, ModelNode remoteDC) throws OperationFailedException {
            // no-op
        }
    }
}
