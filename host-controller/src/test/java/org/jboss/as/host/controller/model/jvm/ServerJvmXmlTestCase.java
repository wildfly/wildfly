/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller.model.jvm;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.parsing.JvmXml;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerJvmXmlTestCase extends ManagementTestSetup {

    @Test
    public void testParseJvm() throws Exception {
        String original = readResource("jvms.xml");
        Parser parser = createParser(new JvmXmlReaderAdapter(Namespace.DOMAIN_1_3), Namespace.DOMAIN_1_3, "jvms");
        parser.installParsedOperations(original);

        compareXml(original, parser.getPersistedXml(), true);

        ModelNode model = readModel(true);
        Assert.assertEquals(2, model.get(JVM).keys().size());

        ModelNode empty = model.get(JVM, "empty");
        Assert.assertTrue(empty.isDefined());
        Assert.assertTrue(empty.keys().size() > 1);
        for (String key : empty.keys()) {
            Assert.assertFalse(empty.get(key).isDefined());
        }

        ModelNode full = model.get(JVM, "full");
        Assert.assertEquals("agentLib", full.get("agent-lib").asString());
        Assert.assertEquals("agentPath", full.get("agent-path").asString());
        Assert.assertEquals(true, full.get("debug-enabled").asBoolean());
        Assert.assertEquals("debugOptions", full.get("debug-options").asString());
        Assert.assertEquals(true, full.get("env-classpath-ignored").asBoolean());
        Assert.assertEquals("heapSize", full.get("heap-size").asString());
        Assert.assertEquals("javaAgent", full.get("java-agent").asString());
        Assert.assertEquals("javaHome", full.get("java-home").asString());
        Assert.assertEquals("maxHeapSize", full.get("max-heap-size").asString());
        Assert.assertEquals("maxPermGenSize", full.get("max-permgen-size").asString());
        Assert.assertEquals("stackSize", full.get("stack-size").asString());
        Assert.assertEquals("SUN", full.get("type").asString());

        List<ModelNode> options = full.get("jvm-options").asList();
        Assert.assertEquals(3, options.size());
        Assert.assertEquals("option1", options.get(0).asString());
        Assert.assertEquals("option2", options.get(1).asString());
        Assert.assertEquals("option3", options.get(2).asString());

        List<ModelNode> environment = full.get("environment-variables").asList();
        Assert.assertEquals(2, environment.size());
        Assert.assertEquals("name1", environment.get(0).asProperty().getName());
        Assert.assertEquals("value1", environment.get(0).asProperty().getValue().asString());
        Assert.assertEquals("name2", environment.get(1).asProperty().getName());
        Assert.assertEquals("value2", environment.get(1).asProperty().getValue().asString());
    }

    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        super.initModel(rootResource, registration);
        registration.registerSubModel(JvmResourceDefinition.SERVER);
    }

    static class JvmXmlReaderAdapter implements XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelMarshallingContext> {
        final Set<String> jvmNames = new HashSet<String>();
        final Namespace namespace;

        public JvmXmlReaderAdapter(Namespace namespace) {
            this.namespace = namespace;
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> updates) throws XMLStreamException {
            if (!reader.getLocalName().equals("jvms")) {
                throw new IllegalStateException("expected <jvms/>");
            }
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                requireNamespace(reader, namespace);
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case JVM: {
                        JvmXml.parseJvm(reader, new ModelNode().setEmptyList(), namespace, updates, jvmNames, true);
                        break;
                    }

                    default:
                        throw unexpectedElement(reader);
                }
            }
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, ModelMarshallingContext context) throws XMLStreamException {
            final ModelNode modelNode = context.getModelNode();
            if (modelNode.hasDefined(JVM)) {
                writer.writeStartElement(Element.JVMS.getLocalName());
                for (final Property jvm : modelNode.get(JVM).asPropertyList()) {
                    JvmXml.writeJVMElement(writer, jvm.getName(), jvm.getValue());
                }
                writer.writeEndElement();
            }
        }
    }
}
