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
package org.jboss.as.test.surefire.xml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.namespace.QName;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.DomainXml;
import org.jboss.as.controller.parsing.HostXml;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.StandaloneXml;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainModelImpl;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.LocalHostModel;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.server.ServerControllerModelUtil;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ParseAndMarshalModelsTestCase {

    @Test
    public void testStandaloneXml() throws Exception {
        File file = new File("target/standalone-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(getOriginalStandaloneXml(), file);
        ModelNode originalModel = loadServerModel(file);
        ModelNode reparsedModel = loadServerModel(file);

        fixupDs(originalModel);
        fixupOSGiStandalone(originalModel, reparsedModel);

        compare(originalModel, reparsedModel);
    }

    @Test
    public void testHostXml() throws Exception {
        File file = new File("target/host-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(getOriginalHostXml(), file);
        ModelNode originalModel = loadHostModel(file);
        ModelNode reparsedModel = loadHostModel(file);

        compare(originalModel, reparsedModel);
    }


    @Test
    public void testDomainXml() throws Exception {
        File file = new File("target/domain-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(getOriginalDomainXml(), file);
        ModelNode originalModel = loadDomainModel(file);
        ModelNode reparsedModel = loadDomainModel(file);

        fixupOSGiDomain(originalModel, reparsedModel);
        compare(originalModel, reparsedModel);
    }

    //TODO look into why we get a "set-tx-query-timeout"=>false in the model
    // BES 2011/03/04 reason for this is empty <timeout></timeout> element
    // results in the item in the model due to behavior of IJ's DsParser,
    // but when we marshal we realize there is no need to write the empty
    // <timeout></timeout> element. So reparse doesn't add the element.
    // Solution is to remove the empty element.
    private void fixupDs(ModelNode node) {

        for (ModelNode ds : node.get("subsystem", "datasources", "datasources").asList()) {
            ds.remove("set-tx-query-timeout");
        }
    }

    private void fixupOSGiStandalone(ModelNode node1, ModelNode node2) {
        //These multiline properties get extra indentation when marshalled. Put them on one line to compare properly
        node1.get("subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").set(convertToSingleLine(node1.get("subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").asString()));
        node2.get("subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").set(convertToSingleLine(node2.get("subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").asString()));
        node1.get("subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node1.get("subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").asString()));
        node2.get("subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node2.get("subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").asString()));
    }

    private void fixupOSGiDomain(ModelNode node1, ModelNode node2) {
        //These multiline properties get extra indentation when marshalled. Put them on one line to compare properly
        node1.get("profile", "default", "subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").set(convertToSingleLine(node1.get("profile", "default", "subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").asString()));
        node2.get("profile", "default", "subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").set(convertToSingleLine(node2.get("profile", "default", "subsystem", "osgi", "properties", "org.jboss.osgi.system.modules").asString()));
        node1.get("profile", "default", "subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node1.get("profile", "default", "subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").asString()));
        node2.get("profile", "default", "subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node2.get("profile", "default", "subsystem", "osgi", "properties", "org.osgi.framework.system.packages.extra").asString()));
    }

    private String convertToSingleLine(String value) {
        //Reformat the string so it works better in ParseAndMarshalModelsTestCase
        value.replace('\r', ' ');
        value.replace('\n', ' ');
        String[] values = value.split(",");
        StringBuilder formattedValue = new StringBuilder();
        boolean first = true;
        for (String val : values) {
            if (!first) {
                formattedValue.append(", ");
            } else {
                first = false;
            }
            formattedValue.append(val.trim());
        }
        return formattedValue.toString();
    }

    private void compare(ModelNode node1, ModelNode node2) {
        Assert.assertEquals(node1.getType(), node2.getType());
        if (node1.getType() == ModelType.OBJECT) {
            final Set<String> keys1 = node1.keys();
            final Set<String> keys2 = node1.keys();
            Assert.assertEquals(node1 + "\n" + node2, keys1.size(), keys2.size());

            for (String key : keys1) {
                final ModelNode child1 = node1.get(key);
                Assert.assertTrue(node1 + "\n" + node2, node2.has(key));
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

            for (int i = 0 ; i < list1.size() ; i++) {
                compare(list1.get(i), list2.get(i));
            }

        } else if (node1.getType() == ModelType.PROPERTY) {
            Property prop1 = node1.asProperty();
            Property prop2 = node2.asProperty();
            Assert.assertEquals(prop1 + "\n" + prop2, prop1.getName(), prop2.getName());
            compare(prop1.getValue(), prop2.getValue());

        } else {
            try {
                Assert.assertEquals("\n\"" + node1.asString() + "\"\n\"" + node2.asString() + "\"\n-----", node2.asString().trim(), node1.asString().trim());
            } catch (AssertionFailedError error) {
                throw error;
            }
        }
    }

    private ModelNode loadServerModel(File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
        final StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader());
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        final List<ModelNode> ops = persister.load();

        TestServerController controller = new TestServerController(persister);
        executeOperations(controller, ops);

        ModelNode model = controller.getModel().clone();
        persister.store(model);
        return model;
    }

    private ModelNode loadHostModel(File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "host");
        final HostXml parser = new HostXml(Module.getBootModuleLoader());
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        final List<ModelNode> ops = persister.load();

        TestHostController controller = new TestHostController(persister);
        executeOperations(controller, ops);

        ModelNode model = controller.getHostModel();
        persister.store(model);
        return model;
    }

    private ModelNode loadDomainModel(File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "domain");
        final DomainXml parser = new DomainXml(Module.getBootModuleLoader());
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        final List<ModelNode> ops = persister.load();

        TestDomainController controller = new TestDomainController(persister);
        executeOperations(controller, ops);

        ModelNode model = controller.getDomainModel().clone();
        persister.store(model);
        return model;
    }

    private void executeOperations(ModelController controller, List<ModelNode> ops) {
        for (final ModelNode op : ops) {
            controller.execute(OperationBuilder.Factory.create(op).build(),
                    new org.jboss.as.controller.ResultHandler() {

                @Override
                public void handleResultFragment(String[] location, ModelNode result) {
                }

                @Override
                public void handleResultComplete() {
                }

                @Override
                public void handleFailed(ModelNode failureDescription) {
                    throw new IllegalArgumentException(op + " " + failureDescription.toString());
                }

                @Override
                public void handleCancellation() {
                }
            });
        }
    }

    private File getOriginalStandaloneXml() {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = new File(".").getAbsoluteFile();
        f = f.getParentFile().getParentFile().getParentFile();
        Assert.assertTrue(f.exists());
        f = new File(f, "build");
        Assert.assertTrue(f.exists());
        f = new File(f, "src");
        Assert.assertTrue(f.exists());
        f = new File(f, "main");
        Assert.assertTrue(f.exists());
        f = new File(f, "resources");
        Assert.assertTrue(f.exists());
        f = new File(f, "standalone");
        Assert.assertTrue(f.exists());
        f = new File(f, "configuration");
        Assert.assertTrue(f.exists());
        f = new File(f, "standalone.xml");
        Assert.assertTrue(f.exists());
        return f;
    }

    private File getOriginalHostXml() {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = new File(".").getAbsoluteFile();
        f = f.getParentFile().getParentFile().getParentFile();
        Assert.assertTrue(f.exists());
        f = new File(f, "build");
        Assert.assertTrue(f.exists());
        f = new File(f, "src");
        Assert.assertTrue(f.exists());
        f = new File(f, "main");
        Assert.assertTrue(f.exists());
        f = new File(f, "resources");
        Assert.assertTrue(f.exists());
        f = new File(f, "domain");
        Assert.assertTrue(f.exists());
        f = new File(f, "configuration");
        Assert.assertTrue(f.exists());
        f = new File(f, "host.xml");
        Assert.assertTrue(f.exists());
        return f;
    }

    private File getOriginalDomainXml() {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = new File(".").getAbsoluteFile();
        f = f.getParentFile().getParentFile().getParentFile();
        Assert.assertTrue(f.exists());
        f = new File(f, "build");
        Assert.assertTrue(f.exists());
        f = new File(f, "src");
        Assert.assertTrue(f.exists());
        f = new File(f, "main");
        Assert.assertTrue(f.exists());
        f = new File(f, "resources");
        Assert.assertTrue(f.exists());
        f = new File(f, "domain");
        Assert.assertTrue(f.exists());
        f = new File(f, "configuration");
        Assert.assertTrue(f.exists());
        f = new File(f, "domain.xml");
        Assert.assertTrue(f.exists());
        return f;
    }

    private void copyFile(final File src, final File dest) throws Exception {
        final InputStream in = new BufferedInputStream(new FileInputStream(src));
        try {
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

    private static class TestServerController extends BasicModelController {

        protected TestServerController(ExtensibleConfigurationPersister configurationPersister) {
            super(ServerControllerModelUtil.createCoreModel(), configurationPersister, new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    return new ModelNode();
                }
            });

            ServerControllerModelUtil.initOperations(
                    getRegistry(),
                    new DeploymentRepository() {

                        @Override
                        public boolean hasDeploymentContent(byte[] hash) {
                            return false;
                        }

                        @Override
                        public byte[] addDeploymentContent(InputStream stream) throws IOException {
                            return null;
                        }
                    },
                    configurationPersister);
        }

        @Override
        protected ModelNode getModel() {
            return super.getModel();
        }

        @Override
        protected void persistConfiguration(ModelNode model,
                ConfigurationPersisterProvider configurationPersisterFactory) {
            // ignore
        }
    }

    private static class TestHostController extends BasicModelController {

        public TestHostController(ExtensibleConfigurationPersister configurationPersister) {
            super(HostModelUtil.createCoreModel(), configurationPersister, HostModelUtil.createHostRegistry(configurationPersister));
        }

        public ModelNode getHostModel() {
            return getModel().clone();
        }

        @Override
        protected void persistConfiguration(ModelNode model,
                ConfigurationPersisterProvider configurationPersisterFactory) {
            // ignore
        }
    }

    private static class TestDomainController extends DomainModelImpl {
        protected TestDomainController(ExtensibleConfigurationPersister configurationPersister) {
            super(configurationPersister, MockHostControllerProxy.INSTANCE, MockDeploymentRepository.INSTANCE, MockFileRepository.INSTANCE);
        }

        @Override
        public ModelNode getDomainModel() {
            return super.getDomainModel();
        }

        @Override
        protected void persistConfiguration(ModelNode model,
                ConfigurationPersisterProvider configurationPersisterFactory) {
            // ignore
        }
    }

    private static class MockDeploymentRepository implements DeploymentRepository {

        private static final MockDeploymentRepository INSTANCE = new MockDeploymentRepository();

        @Override
        public byte[] addDeploymentContent(InputStream stream) throws IOException {
            return null;
        }

        @Override
        public boolean hasDeploymentContent(byte[] hash) {
            return true;
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

    private static class MockHostControllerProxy implements LocalHostModel {

        private static final MockHostControllerProxy INSTANCE = new MockHostControllerProxy();

        @Override
        public String getName() {
            return "mock";
        }

        @Override
        public ModelNode getHostModel() {
            return new ModelNode();
        }

        @Override
        public ModelNodeRegistration getRegistry() {
            return ModelNodeRegistration.Factory.create(new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    return new ModelNode();
                }});
        }

        @Override
        public void startServers(DomainController domainController) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stopServers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ExtensibleConfigurationPersister getConfigurationPersister() {
            return new ExtensibleConfigurationPersister(){

                @Override
                public void store(ModelNode model) throws ConfigurationPersistenceException {
                }

                @Override
                public void marshallAsXml(ModelNode model, OutputStream output)
                        throws ConfigurationPersistenceException {
                }

                @Override
                public List<ModelNode> load() throws ConfigurationPersistenceException {
                    return null;
                }

                @Override
                public void registerSubsystemWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer) {
                }

                @Override
                public void registerSubsystemDeploymentWriter(String name,
                        XMLElementWriter<SubsystemMarshallingContext> writer) {
                }};
        }

    }
}
