/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.base;

import java.io.StringReader;
import java.util.Arrays;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.as.model.DeploymentUnitKey;
import org.jboss.as.model.Element;
import org.jboss.as.model.base.util.MockRootElement;
import org.jboss.as.model.base.util.MockRootElementParser;
import org.jboss.as.model.base.util.TestXMLElementReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link DeploymentUnitElement}.
 * 
 * @author Brian Stansberry
 */
public abstract class DeploymentUnitElementTestBase extends DomainModelElementTestBase {

    private static final byte[] SHA1_HASH = AbstractModelElement.hexStringToByteArray("22cfd207b9b90e0014a4");
    private static final DeploymentUnitKey KEY = new DeploymentUnitKey("my-war.ear", SHA1_HASH);
    
    /**
     * @param name
     */
    public DeploymentUnitElementTestBase(String name) {
        super(name);
    }
    
    protected XMLMapper createXMLMapper() throws Exception{

        XMLMapper mapper = XMLMapper.Factory.create();
        MockRootElementParser.registerXMLElementReaders(mapper, getTargetNamespace());
        mapper.registerRootElement(new QName(getTargetNamespace(), Element.DEPLOYMENT.getLocalName()), 
                new TestXMLElementReader<DeploymentUnitElement>(DeploymentUnitElement.class));  
        return mapper;
    }

    public void testSimpleParse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        DeploymentUnitElement testee = (DeploymentUnitElement) root.getChild(getTargetNamespace(), Element.DEPLOYMENT.getLocalName());
        assertEquals("my-war.ear", testee.getName());
        assertTrue(Arrays.equals(SHA1_HASH, testee.getSha1Hash()));
        assertEquals(KEY, testee.getKey());
        assertTrue(testee.isAllowed());
        assertTrue(testee.isStart());
    }

    public void testFullParse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\" allowed=\"false\" start=\"false\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        DeploymentUnitElement testee = (DeploymentUnitElement) root.getChild(getTargetNamespace(), Element.DEPLOYMENT.getLocalName());
        assertEquals("my-war.ear", testee.getName());
        assertTrue(Arrays.equals(SHA1_HASH, testee.getSha1Hash()));
        assertEquals(KEY, testee.getKey());
        assertFalse(testee.isAllowed());
        assertFalse(testee.isStart());
    }
    
    public void testNoNameParse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Missing 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }
    
    public void testNoSha1Parse() throws Exception {
        String testContent = "<deployment sha1=\"22cfd207b9b90e0014a4\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Missing 'sha1' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }
    
    public void testBadSha1Parse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" sha1=\"xxx\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Missing 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }
    
    public void testBadAttributeParse() throws Exception {
        String testContent = "<deployment bogus=\"foo\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
        
        testContent = "<deployment name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\" bogus=\"foo\"/>";
        fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadChildElement() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\"><bogus/></deployment>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        
        try {
            MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
            fail("Extraneous child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }
    
    public void testSerializationDeserialization() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\" allowed=\"false\" start=\"false\"/>";
        String fullcontent = MockRootElement.getXmlContent(getTargetNamespace(), getTargetNamespaceLocation(), false, testContent);
        MockRootElement root = MockRootElementParser.parseRootElement(getXMLMapper(), new StringReader(fullcontent));
        DeploymentUnitElement testee = (DeploymentUnitElement) root.getChild(getTargetNamespace(), Element.DEPLOYMENT.getLocalName());
        
        byte[] bytes = serialize(testee);        
        DeploymentUnitElement testee1 = deserialize(bytes, DeploymentUnitElement.class);

        assertEquals(testee.elementHash(), testee1.elementHash());
        assertEquals(testee.getName(), testee1.getName());
        assertTrue(Arrays.equals(testee.getSha1Hash(), testee1.getSha1Hash()));
        assertEquals(testee.getKey(), testee1.getKey());
        assertEquals(testee.isAllowed(), testee1.isAllowed());
        assertEquals(testee.isStart(), testee1.isStart());
    }

}
