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

import java.util.Arrays;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.as.model.ModelXmlParsers;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.model.base.util.MockRootElementParser;
import org.jboss.as.model.base.util.ModelParsingSupport;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit tests of {@link DeploymentUnitElement}.
 *
 * @author Brian Stansberry
 */
public abstract class DeploymentUnitElementTestBase extends DomainModelElementTestBase {

    private static final byte[] SHA1_HASH = ParseUtils.hexStringToByteArray("22cfd207b9b90e0014a4");

    /**
     * @param name
     */
    public DeploymentUnitElementTestBase(String name) {
        super(name);
    }

    @Override
    protected XMLMapper createXMLMapper() throws Exception{

        XMLMapper mapper = XMLMapper.Factory.create();
        MockRootElementParser.registerXMLElementReaders(mapper, getTargetNamespace());
        mapper.registerRootElement(new QName(getTargetNamespace(), Element.DOMAIN.getLocalName()),
                ModelXmlParsers.DOMAIN_XML_READER);
        return mapper;
    }

    public void testSimpleParse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear_v1\" runtime-name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);
        DomainModel root = ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
        DeploymentUnitElement testee = root.getDeployment("my-war.ear_v1");

        assertEquals("my-war.ear", testee.getRuntimeName());
        assertTrue(Arrays.equals(SHA1_HASH, testee.getSha1Hash()));
        assertEquals("my-war.ear_v1", testee.getUniqueName());
        assertTrue(testee.isStart());
    }

    public void testFullParse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear_v1\" runtime-name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\" start=\"false\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);
        DomainModel root = ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
        DeploymentUnitElement testee = root.getDeployment("my-war.ear_v1");

        assertEquals("my-war.ear", testee.getRuntimeName());
        assertTrue(Arrays.equals(SHA1_HASH, testee.getSha1Hash()));
        assertEquals("my-war.ear_v1", testee.getUniqueName());
        assertFalse(testee.isStart());
    }

    public void testNoHashParse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" runtime-name=\"my-war.ear\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testNoUniqueNameParse() throws Exception {
        String testContent = "<deployment runtime-name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing 'sha1' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testNoRuntimeNameParse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing 'sha1' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadSha1Parse() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" runtime-name=\"my-war.ear\" sha1=\"xxx\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Missing 'name' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadAttributeParse() throws Exception {
        String testContent = "<deployment bogus=\"foo\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }

        testContent = "<deployment name=\"my-war.ear\" runtime-name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\" bogus=\"foo\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Extraneous 'bogus' attribute did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    public void testBadChildElement() throws Exception {
        String testContent = "<deployment name=\"my-war.ear\" runtime-name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\"><bogus/></deployment>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);

        try {
            ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
            fail("Extraneous child element did not cause parsing failure");
        }
        catch (XMLStreamException good) {
            // TODO validate the location stuff in the exception message
        }
    }

    @Override
    public void testSerializationDeserialization() throws Exception {
        String testContent = "<deployment name=\"my-war.ear_v1\" runtime-name=\"my-war.ear\" sha1=\"22cfd207b9b90e0014a4\" start=\"false\"/>";
        testContent = ModelParsingSupport.wrap(Element.DEPLOYMENTS.getLocalName(), testContent);
        String fullcontent = ModelParsingSupport.getXmlContent(Element.DOMAIN.getLocalName(), getTargetNamespace(), getTargetNamespaceLocation(), testContent);
        DomainModel root = ModelParsingSupport.parseDomainModel(getXMLMapper(), fullcontent);
        DeploymentUnitElement testee = root.getDeployment("my-war.ear_v1");

        byte[] bytes = serialize(testee);
        DeploymentUnitElement testee1 = deserialize(bytes, DeploymentUnitElement.class);

        assertEquals(testee.getRuntimeName(), testee1.getRuntimeName());
        assertTrue(Arrays.equals(testee.getSha1Hash(), testee1.getSha1Hash()));
        assertEquals(testee.getUniqueName(), testee1.getUniqueName());
        assertEquals(testee.isStart(), testee1.isStart());
    }

}
