/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.as.test.shared.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createCompositeNode;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public abstract class AbstractMgmtTestBase {

    private static File brokenWar = null;

    protected abstract ModelControllerClient getModelControllerClient();

    protected ModelNode executeOperation(final ModelNode op, boolean unwrapResult) throws IOException, MgmtOperationException {
        ModelNode ret = getModelControllerClient().execute(op);
        if (!unwrapResult) return ret;

        if (!SUCCESS.equals(ret.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Management operation failed: " + ret.get(FAILURE_DESCRIPTION), op, ret);
        }
        return ret.get(RESULT);
    }

    protected ModelNode executeOperation(final ModelNode op) throws IOException, MgmtOperationException {
        return executeOperation(op, true);
    }

    protected ModelNode executeOperation(final String address, final String operation) throws IOException, MgmtOperationException {
        return executeOperation(createOpNode(address, operation));
    }

    protected ModelNode executeAndRollbackOperation(final ModelNode op) throws IOException, OperationFormatException {

        ModelNode addDeploymentOp = createOpNode("deployment=malformedDeployment.war", "add");
        addDeploymentOp.get("content").get(0).get("input-stream-index").set(0);

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("deploy");
        builder.addNode("deployment", "malformedDeployment.war");


        ModelNode[] steps = new ModelNode[3];
        steps[0] = op;
        steps[1] = addDeploymentOp;
        steps[2] = builder.buildRequest();
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(new FileInputStream(getBrokenWar()));

        return getModelControllerClient().execute(ob.build());
    }

    protected void remove(final ModelNode address) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);
    }

    private File getBrokenWar() {
        if (brokenWar != null) return brokenWar;

        WebArchive war = ShrinkWrap.create(WebArchive.class, "deployment2.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(new StringAsset("Malformed"), "web.xml");
        brokenWar = new File(System.getProperty("java.io.tmpdir") + File.separator + "malformedDeployment.war");
        brokenWar.deleteOnExit();
        new ZipExporterImpl(war).exportTo(brokenWar, true);
        return brokenWar;
    }

    protected Map<String, ModelNode> getChildren(final ModelNode result) {
        assert result.isDefined();
        final Map<String, ModelNode> steps = new HashMap<String, ModelNode>();
        for (final Property property : result.asPropertyList()) {
            steps.put(property.getName(), property.getValue());
        }
        return steps;
    }

    protected ModelNode findNodeWithProperty(List<ModelNode> newList, String propertyName, String setTo) {
        ModelNode toReturn = null;
        for (ModelNode result : newList) {
            final Map<String, ModelNode> parseChildren = getChildren(result);
            if (!parseChildren.isEmpty() && parseChildren.get(propertyName) != null && parseChildren.get(propertyName).asString().equals(setTo)) {
                toReturn = result;
                break;
            }
        }
        return toReturn;
    }

    public String modelToXml(String subsystemName, String childType, XMLElementWriter<SubsystemMarshallingContext> parser) throws Exception {
        final ModelNode address = new ModelNode();
        address.add("subsystem", subsystemName);
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-resources");
        operation.get("child-type").set(childType);
        operation.get(RECURSIVE).set(true);
        operation.get(OP_ADDR).set(address);

        final ModelNode result = executeOperation(operation);
        Assert.assertNotNull(result);

        ModelNode dsNode = new ModelNode();
        dsNode.get(childType).set(result);

        StringWriter strWriter = new StringWriter();
        XMLExtendedStreamWriter writer = XMLExtendedStreamWriterFactory.create(XMLOutputFactory.newFactory()
                .createXMLStreamWriter(strWriter));
        parser.writeContent(writer, new SubsystemMarshallingContext(dsNode, writer));
        writer.flush();
        return strWriter.toString();
    }

    public static List<ModelNode> xmlToModelOperations(String xml, String nameSpaceUriString, XMLElementReader<List<ModelNode>> parser) throws Exception {
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(nameSpaceUriString, "subsystem"), parser);

        StringReader strReader = new StringReader(xml);

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(strReader));
        List<ModelNode> newList = new ArrayList<ModelNode>();
        mapper.parseDocument(newList, reader);

        return newList;
    }

    public static ModelNode operationListToCompositeOperation(List<ModelNode> operations) {
        return operationListToCompositeOperation(operations, true);
    }

    public static ModelNode operationListToCompositeOperation(List<ModelNode> operations, boolean skipFirst) {
        if (skipFirst) operations.remove(0);
        ModelNode[] steps = new ModelNode[operations.size()];
        operations.toArray(steps);
        return createCompositeNode(steps);
    }

    public static String readXmlResource(final String name) throws IOException {
        File f = new File(name);
        BufferedReader reader = new BufferedReader(new FileReader(f));
        StringWriter writer = new StringWriter();
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
        }
        return writer.toString();
    }
    
    protected void takeSnapShot() throws Exception{
    	final ModelNode operation0 = new ModelNode();
        operation0.get(OP).set("take-snapshot");
        
        executeOperation(operation0);
    }
}
