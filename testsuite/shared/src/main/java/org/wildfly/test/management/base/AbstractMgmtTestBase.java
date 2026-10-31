/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.management.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createCompositeNode;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public abstract class AbstractMgmtTestBase {

    protected abstract ModelControllerClient getModelControllerClient();

    protected ModelNode executeOperation(final ModelNode op, boolean unwrapResult) throws IOException, MgmtOperationException {
        if(unwrapResult) {
            return ManagementOperations.executeOperation(getModelControllerClient(), op);
        } else {
            return ManagementOperations.executeOperationRaw(getModelControllerClient(), op);
        }
    }

    protected void executeOperations(final List<ModelNode> operations) throws IOException, MgmtOperationException {
        for(ModelNode op : operations) {
            executeOperation(op);
        }
    }
    protected ModelNode executeOperation(final ModelNode op) throws IOException, MgmtOperationException {
        return executeOperation(op, true);
    }

    protected ModelNode executeOperation(final String address, final String operation) throws IOException, MgmtOperationException {
        return executeOperation(createOpNode(address, operation));
    }

    protected ModelNode executeAndRollbackOperation(final ModelNode op) throws IOException {

        ModelNode broken = Util.createOperation("read-attribute", PathAddress.EMPTY_ADDRESS);
        broken.get("no-such-attribute").set("fail");

        /*ModelNode addDeploymentOp = createOpNode("deployment=malformedDeployment.war", "add");
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
        ob.addInputStream(new FileInputStream(getBrokenWar()));*/

        ModelNode[] steps = new ModelNode[2];
        steps[0] = op;
        steps[1] = broken;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);
        OperationBuilder ob = new OperationBuilder(compositeOp, true);

        return getModelControllerClient().execute(ob.build());
    }

    protected void remove(final ModelNode address) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);
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
        assertThat(result).isNotNull();

        ModelNode dsNode = new ModelNode();
        dsNode.get(childType).set(result);

        StringWriter strWriter = new StringWriter();
        XMLExtendedStreamWriter writer = XMLExtendedStreamWriterFactory.create(XMLOutputFactory.newInstance()
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
        Path f = Paths.get(name);
        try (BufferedReader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8);
             StringWriter writer = new StringWriter()) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            return writer.toString();
        }
    }

    protected void takeSnapShot() throws Exception{
        final ModelNode operation0 = new ModelNode();
        operation0.get(OP).set("take-snapshot");

        executeOperation(operation0);
    }
}
