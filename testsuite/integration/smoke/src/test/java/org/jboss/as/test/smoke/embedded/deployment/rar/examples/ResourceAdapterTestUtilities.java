package org.jboss.as.test.smoke.embedded.deployment.rar.examples;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createCompositeNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;

public class ResourceAdapterTestUtilities {
	public static String RAModelToXml(final String childType,ModelControllerClient client)throws Exception {
    	final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-resources");
        operation.get("child-type").set(childType);
        operation.get(RECURSIVE).set(true);
        operation.get(OP_ADDR).set(address);
        final ModelNode ret = client.execute(operation);
        Assert.assertTrue("Management operation " + operation.asString() + " failed: " + ret.asString(),
                SUCCESS.equals(ret.get(OUTCOME).asString()));
        final ModelNode result = ret.get(RESULT);
        Assert.assertNotNull(result);

        ModelNode dsNode = new ModelNode();
        dsNode.get(childType).set(result);
        
        StringWriter strWriter = new StringWriter();
        XMLExtendedStreamWriter writer = XMLExtendedStreamWriterFactory.create(XMLOutputFactory.newFactory()
                .createXMLStreamWriter(strWriter));
        ResourceAdapterSubsystemParser parser = new ResourceAdapterSubsystemParser();
        parser.writeContent(writer, new SubsystemMarshallingContext(dsNode, writer));
        writer.flush();
        return strWriter.toString();
    }
    public static List<ModelNode> XmlToRAModelOperations(String xml) throws Exception {
    	ResourceAdapterSubsystemParser parser = new ResourceAdapterSubsystemParser();
    	XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), "subsystem"), parser);
        
        StringReader strReader = new StringReader(xml);
       
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(strReader));
        List<ModelNode> newList = new ArrayList<ModelNode>();
        mapper.parseDocument(newList, reader);
        
        return newList;
    }
    public static ModelNode operationListToCompositeOperation(List<ModelNode> operations){
    	return operationListToCompositeOperation(operations,true);
    }
    public static ModelNode operationListToCompositeOperation(List<ModelNode> operations, boolean skipFirst){
    	if(skipFirst) operations.remove(0);
    	ModelNode steps[]=new ModelNode[operations.size()];
    	operations.toArray(steps);
    	return createCompositeNode(steps);
    }
    public static String readResource(final String name) throws IOException {
    	File f=new File(name);
        BufferedReader reader = new BufferedReader(new FileReader(f));
        StringWriter writer = new StringWriter();
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
        }
        return writer.toString();
    }

}
