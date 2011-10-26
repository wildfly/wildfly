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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.modcluster.ModClusterSubsystemElementParser;
import org.jboss.as.modcluster.Namespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
// import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
// import org.jboss.staxmapper.FormattingXMLStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Test;

// Test mod_cluster parser

public class ParseAndMarshalModelsTestCase extends TestCase {

    private ModClusterSubsystemElementParser parser = new ModClusterSubsystemElementParser();

    @Test
    public void testStandaloneXml() throws Exception {
        File file = new File("target/copy_standalonemodcluster.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(getOriginalFile("standalonemodcluster.xml"), file);
        ModelNode originalModel = loadServerModel(file);
        ModelNode reparsedModel = loadServerModel(file);

        if (!compare(originalModel, reparsedModel))
            fail("The node changed...");
    }

    @Test
    public void testDynamicLoadProvider() throws Exception {
        File file = new File("target/copy_dynamic-load-provider.xml");
        if (file.exists()) {
            file.delete();
        }
        copyFile(getOriginalFile("dynamic-load-provider.xml"), file);

        ModelNode originalModel = loadServerModel(file);
        ModelNode reparsedModel = loadServerModel(file);

        if (!compare(originalModel, reparsedModel))
            fail("The node changed...");
    }

    private File getOriginalFile(String name) {
        File f = new File(".").getAbsoluteFile();
        f = new File(f, "src");
        Assert.assertTrue(f.exists());
        f = new File(f, "test");
        Assert.assertTrue(f.exists());
        f = new File(f, "resources");
        Assert.assertTrue(f.exists());
        f = new File(f, name);
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

    private boolean compare(ModelNode originalModel, ModelNode reparsedModel) {
        // TODO Auto-generated method stub
        return originalModel.equals(reparsedModel);
    }

    private ModelNode loadServerModel(File file) throws Exception {
        final List<ModelNode> operations = new ArrayList<ModelNode>();

        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), "subsystem"), this.parser);

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(file));
        mapper.parseDocument(operations, reader);

        final ModelNode ret = operations.get(0);
        /* To Debug...
        PrintWriter writer = new PrintWriter("/tmp/jfclere.txt");
        ret.writeString(writer, false);
        writer.close();
         */

        final ModelNode config = ret.get("mod-cluster-config");
        XMLExtendedStreamWriter streamWriter = new FormattingXMLStreamWriter( XMLOutputFactory.newInstance().createXMLStreamWriter((new FileOutputStream(file))));
        SubsystemMarshallingContext context = new SubsystemMarshallingContext(config, streamWriter);

        mapper.deparseDocument((XMLElementWriter<?>) this.parser, context, streamWriter);
        streamWriter.close();
        return ret;
    }
}
