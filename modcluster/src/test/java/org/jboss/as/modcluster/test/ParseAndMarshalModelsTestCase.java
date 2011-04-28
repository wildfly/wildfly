package org.jboss.as.modcluster.test;

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

import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.StandaloneXml;
import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.as.server.ServerControllerModelUtil;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.junit.Test;

// Test mod_cluster parser

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

        compare(originalModel, reparsedModel);
    }

    private File getOriginalStandaloneXml() {
        File f = new File(".").getAbsoluteFile();
        f = new File(f, "src");
        Assert.assertTrue(f.exists());
        f = new File(f, "test");
        Assert.assertTrue(f.exists());
        f = new File(f, "resources");
        Assert.assertTrue(f.exists());
        f = new File(f, "standalonemodcluster.xml");
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

    private void compare(ModelNode originalModel, ModelNode reparsedModel) {
        // TODO Auto-generated method stub

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
    private void executeOperations(TestServerController controller, List<ModelNode> ops) {
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
                    configurationPersister, null);
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
}
