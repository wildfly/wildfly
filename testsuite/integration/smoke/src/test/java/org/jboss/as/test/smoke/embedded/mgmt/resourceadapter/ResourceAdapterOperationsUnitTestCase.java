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

package org.jboss.as.test.smoke.embedded.mgmt.resourceadapter;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;

import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.setOperationParams;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.raCommonProperties;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.raConnectionProperties;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.raAdminProperties;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.addExtensionProperties;
import static org.jboss.as.test.integration.management.util.ComplexPropertiesParseUtils.checkModelParams;

import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.ResourceAdapterSubsystemParser;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriterFactory;
import org.jboss.staxmapper.XMLMapper;
import org.junit.After;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;


/**
 * Resource adapter operation unit test.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("AS7-3185")
public class ResourceAdapterOperationsUnitTestCase extends AbstractMgmtTestBase {

    
    @Deployment
    public static Archive<?> getDeployment() {
    	initModelControllerClient("localhost",9999);
        return ShrinkWrapUtils.createEmptyJavaArchive("dummy");
    }

    

    @After
    public void tearDown() throws IOException {
    	closeModelControllerClient();
    }
    
    @Test
    public void complexResourceAdapterAddTest() throws Exception{
    	 final ModelNode address = new ModelNode();
         address.add("subsystem", "resource-adapters");
         address.add("resource-adapter", "some.rar");
         address.protect();
         
         Properties params=raCommonProperties();
         
         final ModelNode operation = new ModelNode();
         operation.get(OP).set("add");
         operation.get(OP_ADDR).set(address);
         setOperationParams(operation,params);
         operation.get("beanvalidationgroups").add("Class0");
         operation.get("beanvalidationgroups").add("Class00");
         executeOperation(operation);
         
         final ModelNode address1=address.clone();
         address1.add("config-properties", "Property");
         address1.protect();
         
         final ModelNode operation11 = new ModelNode();
         operation11.get(OP).set("add");
         operation11.get(OP_ADDR).set(address1);
         operation11.get("value").set("A");;

         executeOperation(operation11);
         
         final ModelNode conAddress=address.clone();
         conAddress.add("connection-definitions", "Pool1");
         conAddress.protect();
         
         Properties conParams=raConnectionProperties();
         
         final ModelNode operation2 = new ModelNode();
         operation2.get(OP).set("add");
         operation2.get(OP_ADDR).set(conAddress);
         setOperationParams(operation2,conParams);

         executeOperation(operation2);

         final ModelNode con1Address=conAddress.clone();
         con1Address.add("config-properties", "Property");
         con1Address.protect();
         
         final ModelNode operation21 = new ModelNode();
         operation21.get(OP).set("add");
         operation21.get(OP_ADDR).set(con1Address);
         operation21.get("value").set("B");;

         executeOperation(operation21);   
         
         final ModelNode admAddress=address.clone();
         admAddress.add("admin-objects", "Pool2");
         admAddress.protect();
         
         Properties admParams=raAdminProperties();
         
         final ModelNode operation3 = new ModelNode();
         operation3.get(OP).set("add");
         operation3.get(OP_ADDR).set(admAddress);
         setOperationParams(operation3,admParams);

         executeOperation(operation3);
         
         final ModelNode adm1Address=admAddress.clone();
         adm1Address.add("config-properties", "Property");
         adm1Address.protect();
         
         final ModelNode operation31 = new ModelNode();
         operation31.get(OP).set("add");
         operation31.get(OP_ADDR).set(adm1Address);
         operation31.get("value").set("D");;

         executeOperation(operation31);
         
         List<ModelNode> newList = marshalAndReparseRaResources("resource-adapter");

         remove(address);

         Assert.assertNotNull(newList);

         ModelNode node=findNodeWithProperty(newList,"archive","some.rar");
         Assert.assertNotNull("There is no archive element:"+newList,node);
         Assert.assertTrue("node:"+node.asString()+";\nparams"+params,checkModelParams(node,params));
         Assert.assertEquals("beanvalidationgroups element is incorrect:"+node.get("beanvalidationgroups").asString(),node.get("beanvalidationgroups").asString(), "[\"Class0\",\"Class00\"]");
         
         node=findNodeWithProperty(newList,"jndi-name","java:jboss/name1");
         Assert.assertNotNull("There is no connection jndi-name element:"+newList,node);
         Assert.assertTrue("node:"+node.asString()+";\nparams"+conParams,checkModelParams(node,conParams));
         
         node=findNodeWithProperty(newList,"jndi-name","java:jboss/Name3");
         Assert.assertNotNull("There is no admin jndi-name element:"+newList,node);
         Assert.assertTrue("node:"+node.asString()+";\nparams"+admParams,checkModelParams(node,admParams));
         
         node=findNodeWithProperty(newList,"value","D");
         Assert.assertNotNull("There is no admin-object config-property element:"+newList,node);
         
         Map<String, ModelNode> parseChildren = getChildren(node.get("address"));
         Assert.assertEquals(parseChildren.get("admin-objects").asString(),"Pool2");
         Assert.assertEquals(parseChildren.get("config-properties").asString(),"Property");
         
         node=findNodeWithProperty(newList,"value","A");
         Assert.assertNotNull("There is no resource-adapter config-property element:"+newList,node);
         
          parseChildren = getChildren(node.get("address")); 
         Assert.assertEquals(parseChildren.get("resource-adapter").asString(),"some.rar");
         Assert.assertEquals(parseChildren.get("config-properties").asString(),"Property");
         
         node=findNodeWithProperty(newList,"value","B");
         Assert.assertNotNull("There is no connection config-property element:"+newList,node);
         
          parseChildren = getChildren(node.get("address"));
         Assert.assertEquals(parseChildren.get("connection-definitions").asString(),"Pool1");
         Assert.assertEquals(parseChildren.get("config-properties").asString(),"Property");
    } 

    public List<ModelNode> marshalAndReparseRaResources(final String childType) throws Exception {

        final ModelNode address = new ModelNode();
        address.add("subsystem", "resource-adapters");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-children-resources");
        operation.get("child-type").set(childType);
        operation.get(RECURSIVE).set(true);
        operation.get(OP_ADDR).set(address);

        final ModelNode result = executeOperation(operation);
        ModelNode dsNode = new ModelNode();
        dsNode.get(childType).set(result);
        
        StringWriter strWriter = new StringWriter();
        XMLExtendedStreamWriter writer = XMLExtendedStreamWriterFactory.create(XMLOutputFactory.newFactory()
                .createXMLStreamWriter(strWriter));
        ResourceAdapterSubsystemParser parser = new ResourceAdapterSubsystemParser();
        parser.writeContent(writer, new SubsystemMarshallingContext(dsNode, writer));
        writer.flush();
        
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), "subsystem"), parser);
        
        StringReader strReader = new StringReader(strWriter.toString());
       
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(strReader));
        List<ModelNode> newList = new ArrayList<ModelNode>();
        mapper.parseDocument(newList, reader);
        
        return newList;
    }

}
