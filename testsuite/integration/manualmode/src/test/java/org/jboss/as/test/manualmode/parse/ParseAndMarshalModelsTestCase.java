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
package org.jboss.as.test.manualmode.parse;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.xml.namespace.QName;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessState.State;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.resources.DomainRootDefinition;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.model.host.HostResourceDefinition;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceRemoveHandler;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.parsing.DomainXml;
import org.jboss.as.host.controller.parsing.HostXml;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.host.controller.resources.ServerConfigResourceDefinition;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.security.vault.RuntimeVaultReader;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.Location;
import org.jboss.as.server.controller.resources.VaultResourceDefinition;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the ability to parse the config files we ship or have shipped in the past, as well as the ability
 * to marshal them back to xml in a manner such that reparsing them produces a consistent in-memory configuration model.
 * <p/>
 * <b>Note:</b>The standard {@code build/src/main/resources/standalone/configuration} and
 * {@code build/src/main/resources/domain/configuration} files are tested in the smoke integration module ParseAndMarshalModelsTestCase.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class ParseAndMarshalModelsTestCase {

    @Deployment(name = "test", managed = false, testable = true)
    public static Archive<?> getDeployment() {

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bogus.jar");
        archive.addPackage(ParseAndMarshalModelsTestCase.class.getPackage());
        archive.addClass(FileUtils.class);
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
        if (isInContainer()) {
            serviceContainer = ServiceContainer.Factory.create("test");
        }
    }

    @After
    public void cleanup() throws Exception {
        if (isInContainer()) {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName("jboss.msc:type=container,name=test"));
            if (serviceContainer != null) {
                serviceContainer.shutdown();
                try {
                    serviceContainer.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    serviceContainer = null;
                }
            }
        }
    }

    @Test
    @InSequence(-1)
    @RunAsClient
    public void start(@ArquillianResource ContainerController cc, @ArquillianResource Deployer deployer) {
        cc.start("default-jbossas");
        deployer.deploy("test");
    }

    @Test
    @InSequence(1)
    @RunAsClient
    public void stop(@ArquillianResource ContainerController cc, @ArquillianResource Deployer deployer) {
        deployer.undeploy("test");
        cc.stop("default-jbossas");
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
    public void testStandaloneFullXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-full.xml"));
    }

    @Test
    public void testStandaloneFullHAXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-full-ha.xml"));
    }

    @Test
    public void testStandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getDocsExampleConfigFile("standalone-minimalistic.xml"));
    }

    @Test
    public void testStandaloneXtsXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-xts.xml"));
    }

    @Test
    public void testStandaloneHornetqColocatedXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-hornetq-colocated.xml"));
    }

    @Test
    public void testStandaloneJtsXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-jts.xml"));
    }

    @Test
    public void testStandaloneGenericJMSXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-genericjms.xml"));
    }

    @Test
    public void test710StandaloneXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0.xml"));
    }

    @Test
    public void test710StandaloneFullHaXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-full-ha.xml"));
    }

    @Test
    public void test710StandaloneFullXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-full.xml"));
    }

    @Test
    public void test710StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-hornetq-colocated.xml"));
    }

    @Test
    public void test710StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-jts.xml"));
    }

    @Test
    public void test710StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-minimalistic.xml"));
    }

    @Test
    public void test710StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-xts.xml"));
    }

    @Test
    public void test711StandaloneXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1.xml"));
    }

    @Test
    public void test711StandaloneFullHaXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-full-ha.xml"));
    }

    @Test
    public void test711StandaloneFullXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-full.xml"));
    }

    @Test
    public void test711StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-hornetq-colocated.xml"));
    }

    @Test
    public void test711StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-jts.xml"));
    }

    @Test
    public void test711StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-minimalistic.xml"));
    }

    @Test
    public void test711StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-xts.xml"));
    }

    @Test
    public void test712StandaloneXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test712StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test712StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test712StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-hornetq-colocated.xml"));
    }

    @Test
    public void test712StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-jts.xml"));
    }

    @Test
    public void test712StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-minimalistic.xml"));
    }

    @Test
    public void test712StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-xts.xml"));
    }

    @Test
    public void test713StandaloneXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test713StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test713StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test713StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-hornetq-colocated.xml"));
    }

    @Test
    public void test713StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-jts.xml"));
    }

    @Test
    public void test713StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-minimalistic.xml"));
    }

    @Test
    public void test713StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-xts.xml"));
    }

    @Test
    public void test720StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test720StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test720StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-hornetq-colocated.xml"));
    }

    @Test
    public void test720StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-jts.xml"));
    }

    @Test
    public void test720StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-minimalistic.xml"));
    }

    @Test
    public void test720StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-xts.xml"));
    }

    @Test
    public void testEAP620StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP620StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP620StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-hornetq-colocated.xml"));
    }

    @Test
    public void testEAP620StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-jts.xml"));
    }

    @Test
    public void testEAP620StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-minimalistic.xml"));
    }

    @Test
    public void testEAP620StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-xts.xml"));
    }

    private ModelNode standaloneXmlTest(File original) throws Exception {

        File file = new File("target/standalone-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        FileUtils.copyFile(original, file);
        ModelNode originalModel = loadServerModel(file);

        ModelNode reparsedModel = loadServerModel(file);

        compare(originalModel, reparsedModel);

        return reparsedModel;
    }

    @Test
    public void testHostXml() throws Exception {
        hostXmlTest(getOriginalHostXml("host.xml"));
    }

    @Test
    public void test710HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-0.xml"));
    }

    @Test
    public void test711HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-1.xml"));
    }

    @Test
    public void test712HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-2.xml"));
    }

    @Test
    public void test713HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-3.xml"));
    }

    @Test
    public void test720HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-2-0.xml"));
    }

    @Test
    public void testEAP620HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "eap-6-2-0.xml"));
    }

    private void hostXmlTest(final File original) throws Exception {
        File file = new File("target/host-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        FileUtils.copyFile(original, file);
        ModelNode originalModel = loadHostModel(file);
        ModelNode reparsedModel = loadHostModel(file);

        compare(originalModel, reparsedModel);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void testDomainXml() throws Exception {
        domainXmlTest(getOriginalDomainXml("domain.xml"));
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test710DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-1-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test711DomainXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-1-1.xml"));
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test712DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-1-2.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test713DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-1-3.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test720DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-2-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void testEAP620DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-6-2-0.xml"));
        validateJsfProfiles(model);
    }


    private ModelNode domainXmlTest(File original) throws Exception {
        File file = new File("target/domain-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        FileUtils.copyFile(original, file);
        ModelNode originalModel = loadDomainModel(file);
        ModelNode reparsedModel = loadDomainModel(file);

        compare(originalModel, reparsedModel);

        return reparsedModel;
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
            Assert.assertEquals("\n\"" + node1.asString() + "\"\n\"" + node2.asString() + "\"\n-----", node1.asString().trim(), node2.asString().trim());
        }
    }

    private static void validateJsfProfiles(ModelNode model) {
        Assert.assertTrue(model.hasDefined(PROFILE));
        for (Property prop : model.get(PROFILE).asPropertyList()) {
            validateJsfSubsystem(prop.getValue());
        }
    }

    private static void validateJsfSubsystem(ModelNode model) {
        Assert.assertTrue(model.hasDefined(SUBSYSTEM));
        //Assert.assertTrue(model.get(SUBSYSTEM).hasDefined("jsf")); //we cannot check for it as web subsystem is not present to add jsf one
    }

    private ModelNode loadServerModel(final File file) throws Exception {
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.ADMIN_ONLY), null, null);
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
        final StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader(), null, extensionRegistry);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.domainValues()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "server"), parser);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        final List<ModelNode> ops = persister.load();

        final ModelNode model = new ModelNode();
        final ModelController controller = createController(ProcessType.STANDALONE_SERVER, model, new Setup() {
            @Override
            public void setup(Resource resource, ManagementResourceRegistration rootRegistration, DelegatingConfigurableAuthorizer authorizer) {
                ServerRootResourceDefinition def = new ServerRootResourceDefinition(new MockContentRepository(), persister, null, null, null, null, extensionRegistry, false, MOCK_PATH_MANAGER, null, authorizer, AuditLogger.NO_OP_LOGGER);
                def.registerAttributes(rootRegistration);
                def.registerOperations(rootRegistration);
                def.registerChildren(rootRegistration);
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

    //TODO use HostInitializer & TestModelControllerService
    private ModelNode loadHostModel(final File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "host");
        final HostXml parser = new HostXml("host-controller");
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.domainValues()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "host"), parser);
            }
        }
        final List<ModelNode> ops = persister.load();

//        System.out.println(ops);

        final ModelNode model = new ModelNode();

        final ModelController controller = createController(ProcessType.HOST_CONTROLLER, model, new Setup() {
            public void setup(Resource resource, ManagementResourceRegistration root, DelegatingConfigurableAuthorizer authorizer) {

                final Resource host = Resource.Factory.create();
                resource.registerChild(PathElement.pathElement(HOST, "master"), host);

                // TODO maybe make creating of empty nodes part of the MNR description
                host.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
                host.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());

                final LocalHostControllerInfoImpl hostControllerInfo = new LocalHostControllerInfoImpl(new ControlledProcessState(false), "master");

                // Add of the host itself
                ManagementResourceRegistration hostRegistration = root.registerSubModel(
                        ResourceBuilder.Factory.create(PathElement.pathElement(HOST),new NonResolvingResourceDescriptionResolver()).build());


                // Other root resource operations
                XmlMarshallingHandler xmh = new XmlMarshallingHandler(persister);
                hostRegistration.registerOperationHandler(XmlMarshallingHandler.DEFINITION, xmh);
                hostRegistration.registerOperationHandler(NamespaceAddHandler.DEFINITION, NamespaceAddHandler.INSTANCE);
                hostRegistration.registerOperationHandler(SchemaLocationAddHandler.DEFINITION, SchemaLocationAddHandler.INSTANCE);
                hostRegistration.registerReadOnlyAttribute(HostResourceDefinition.MASTER, IsMasterHandler.INSTANCE);

                // System Properties
                ManagementResourceRegistration sysProps = hostRegistration.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.HOST));

                //vault
                hostRegistration.registerSubModel(new VaultResourceDefinition(new MockVaultReader()));

                // Central Management
                ResourceDefinition nativeDef = new NativeManagementResourceDefinition(hostControllerInfo);
                ResourceDefinition httpDef = new HttpManagementResourceDefinition(hostControllerInfo, null);

                ResourceDefinition core = CoreManagementResourceDefinition.forHost(authorizer, AuditLogger.NO_OP_LOGGER, MOCK_PATH_MANAGER, new EnvironmentNameReader() {

                    @Override
                    public boolean isServer() {
                        return false;
                    }

                    @Override
                    public String getServerName() {
                        return null;
                    }

                    @Override
                    public String getProductName() {
                        return null;
                    }

                    @Override
                    public String getHostName() {
                        return null;
                    }
                }, nativeDef, httpDef);
                hostRegistration.registerSubModel(core);

                // Domain controller
                LocalDomainControllerAddHandler localDcAddHandler = new MockLocalDomainControllerAddHandler();
                hostRegistration.registerOperationHandler(LocalDomainControllerAddHandler.DEFINITION, localDcAddHandler, false);
                RemoteDomainControllerAddHandler remoteDcAddHandler = new MockRemoteDomainControllerAddHandler();
                hostRegistration.registerOperationHandler(RemoteDomainControllerAddHandler.DEFINITION, remoteDcAddHandler, false);

                // Jvms
                final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(JvmResourceDefinition.GLOBAL);

                //Paths
                ManagementResourceRegistration paths = hostRegistration.registerSubModel(PathResourceDefinition.createSpecified(MOCK_PATH_MANAGER));

                //interface
                ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(new InterfaceDefinition(
                        HostSpecifiedInterfaceAddHandler.INSTANCE,
                        HostSpecifiedInterfaceRemoveHandler.INSTANCE,
                        true
                ));

                //server configurations
                hostRegistration.registerSubModel(new ServerConfigResourceDefinition(MOCK_HOST_CONTROLLER_INFO, null, MOCK_PATH_MANAGER));
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
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, new RunningModeControl(RunningMode.NORMAL), null, null);
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "domain");
        final DomainXml parser = new DomainXml(Module.getBootModuleLoader(), null, extensionRegistry);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.domainValues()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "domain"), parser);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        final List<ModelNode> ops = persister.load();


        final ModelNode model = new ModelNode();
        final ModelController controller = createController(ProcessType.HOST_CONTROLLER, model, new Setup() {
            public void setup(Resource resource, ManagementResourceRegistration rootRegistration, DelegatingConfigurableAuthorizer authorizer) {
                DomainRootDefinition def = new DomainRootDefinition(null, null, persister, new MockContentRepository(), new MockFileRepository(), true, null, extensionRegistry, null, MOCK_PATH_MANAGER, null, authorizer);
                def.initialize(rootRegistration);
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
        ModelControllerService svc = new ModelControllerService(processType, registration, model);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await(30, TimeUnit.SECONDS);
        ModelController controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);

        return controller;
    }

    private void executeOperations(ModelController controller, List<ModelNode> ops) {
        for (final ModelNode op : ops) {
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            controller.execute(op, null, null, null);
        }
    }


    //  Get-config methods

    private File getOriginalStandaloneXml(String profile) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jbossas.project.dir", "../../.."),
                "build/target/generated-configs/standalone/configuration/" + profile
        );
    }

    private File getOriginalHostXml(final String profile) throws FileNotFoundException {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = getHostConfigDir();
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getOriginalDomainXml(final String profile) throws FileNotFoundException {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = getDomainConfigDir();
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getLegacyConfigFile(String type, String profile) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jbossas.ts.submodule.dir"),
                "src/test/resources/legacy-configs/" + type + File.separator + profile
        );
    }

    private File getDocsExampleConfigFile(String name) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jbossas.project.dir", "../../.."),
                "build/src/main/resources/docs/examples/configs" + File.separator + name
        );
    }


    private File getGeneratedExampleConfigFile(String name) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jbossas.project.dir", "../../.."),
                "build/target/generated-configs/docs/examples/configs" + File.separator + name
        );
    }

    private File getHostConfigDir() throws FileNotFoundException {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jbossas.project.dir", "../../.."),
                "build/src/main/resources/domain/configuration"
        );
    }

    private File getDomainConfigDir() throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jbossas.project.dir", "../../.."),
                "build/target/generated-configs/domain/configuration"
        );
    }

    private boolean isInContainer() {
        return this.getClass().getClassLoader() instanceof ModuleClassLoader;
    }

    interface Setup {
        void setup(Resource resource, ManagementResourceRegistration rootRegistration, DelegatingConfigurableAuthorizer authorizer);
    }

    class ModelControllerService extends AbstractControllerService {

        private final CountDownLatch latch = new CountDownLatch(2);
        private final ModelNode model;
        private final Setup registration;

        ModelControllerService(final ProcessType processType, final Setup registration, final ModelNode model) {
            super(processType, new RunningModeControl(RunningMode.ADMIN_ONLY), new NullConfigurationPersister(), new ControlledProcessState(true),
                    ResourceBuilder.Factory.create(PathElement.pathElement("root"), new NonResolvingResourceDescriptionResolver()).build(), null, ExpressionResolver.TEST_RESOLVER, AuditLogger.NO_OP_LOGGER, new DelegatingConfigurableAuthorizer());
            this.model = model;
            this.registration = registration;
        }

        @Override
        public void start(StartContext context) throws StartException {
            super.start(context);
            latch.countDown();
        }

        @Override
        protected void bootThreadDone() {
            super.bootThreadDone();
            latch.countDown();
        }

        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            registration.setup(rootResource, rootRegistration, authorizer);

            rootRegistration.registerOperationHandler(new SimpleOperationDefinitionBuilder("capture-model", new NonResolvingResourceDescriptionResolver()).build()
                    , new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    model.set(Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
                }
            });
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
        public boolean syncContent(byte[] hash) {
            return hasContent(hash);
        }

        @Override
        public boolean hasContent(byte[] hash) {
            return false;
        }

        @Override
        public void removeContent(byte[] hash, Object reference) {
        }

        @Override
        public void addContentReference(byte[] hash, Object reference) {
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

    private static class MockVaultReader extends RuntimeVaultReader {
    }

    private static class MockLocalDomainControllerAddHandler extends LocalDomainControllerAddHandler {

        /**
         * Create the ServerAddHandler
         */
        protected MockLocalDomainControllerAddHandler() {
            super(null, null, null, null, null, null, null, MOCK_PATH_MANAGER);
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
            super(null, null, null, null, null, null, null, null, MOCK_PATH_MANAGER);
        }

        @Override
        protected void initializeDomain(OperationContext context, ModelNode remoteDC) throws OperationFailedException {
            // no-op
        }
    }

    private static PathManagerService MOCK_PATH_MANAGER = new PathManagerService() {
    };

    final static LocalHostControllerInfo MOCK_HOST_CONTROLLER_INFO = new LocalHostControllerInfo() {

        @Override
        public boolean isMasterDomainController() {
            return true;
        }

        @Override
        public String getRemoteDomainControllerUsername() {
            return null;
        }

        @Override
        public List<DiscoveryOption> getRemoteDomainControllerDiscoveryOptions() {
            return null;
        }

        @Override
        public State getProcessState() {
            return null;
        }

        @Override
        public String getNativeManagementSecurityRealm() {
            return null;
        }

        @Override
        public int getNativeManagementPort() {
            return 0;
        }

        @Override
        public String getNativeManagementInterface() {
            return null;
        }

        @Override
        public String getLocalHostName() {
            return null;
        }

        @Override
        public String getHttpManagementSecurityRealm() {
            return null;
        }

        @Override
        public int getHttpManagementSecurePort() {
            return 0;
        }

        @Override
        public int getHttpManagementPort() {
            return 0;
        }

        @Override
        public String getHttpManagementInterface() {
            return null;
        }

        @Override
        public String getHttpManagementSecureInterface() {
            return null;
        }

        @Override
        public boolean isRemoteDomainControllerIgnoreUnaffectedConfiguration() {
            return false;
        }
    };
}
