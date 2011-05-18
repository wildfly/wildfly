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
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.jboss.as.modcluster.ModClusterSubsystemElementParser;
import org.jboss.as.modcluster.Namespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Test;

// Test mod_cluster parser

public class ParseAndMarshalModelsTestCase {

    private ModClusterSubsystemElementParser parser = new ModClusterSubsystemElementParser();

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
        final List<ModelNode> operations = new ArrayList<ModelNode>();

        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), "subsystem"), this.parser);

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(file));
        mapper.parseDocument(operations, reader);

        final ModelNode ret = operations.get(0);

        /*
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
        XMLExtendedStreamWriter streamWriter = XMLExtendedStreamWriterFactory.newInstance().createXMLStreamWriter(output);
        // (XMLElementWriter<?> writer = rootDeparser
        SubsystemMarshallingContext context = new SubsystemMarshallingContext(ret, streamWriter);

        mapper.deparseDocument((XMLElementWriter<?>) this.parser, context, streamWriter);
         */
        return ret;
        /*
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
        final StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader());
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        final List<ModelNode> ops = persister.load();

        TestServerController controller = new TestServerController(persister);
        executeOperations(controller, ops);

        ModelNode model = controller.getModel().clone();
        persister.store(model);
        return model;
        */

    }
}
