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

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Element;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.JvmElement;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.base.JvmElementTestCommon.ContentAndElementGetter;
import org.jboss.as.model.base.util.ModelParsingSupport;

/**
 * Test JvmElement as a child of jvms.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class HostServerJvmElementTestBase extends HostModelTestBase {
    JvmElementTestCommon delegate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        delegate = new JvmElementTestCommon(new ContentAndElementGetter(getXMLMapper(), getTargetNamespace(), getTargetNamespaceLocation()) {

            @Override
            public String getFullContent(String testContent) {
                testContent = ModelParsingSupport.wrapServer(testContent);
                String fullcontent = ModelParsingSupport.getXmlContent(Element.HOST.getLocalName(), targetNamespace, targetNamespaceLocation, testContent);
                return fullcontent;
            }

            @Override
            public JvmElement getTestJvmElement(String fullcontent) throws XMLStreamException, FactoryConfigurationError, UpdateFailedException {
                HostModel root = ModelParsingSupport.parseHostModel(mapper, fullcontent);
                ServerElement server = root.getServer("test");
                assertNotNull(server);
                JvmElement jvm = server.getJvm();
                assertNotNull(jvm);
                return jvm;
            }
        });
    }

    /**
     * @param name
     */
    public HostServerJvmElementTestBase(String name) {
        super(name);
    }

    public void testSimpleParse() throws Exception {
        delegate.testSimpleParse();
    }

    public void testJvmInvalidType() throws Exception{
        delegate.testJvmInvalidType();
    }

    public void testJvmDefaultType() throws Exception {
        delegate.testJvmDefaultType();
    }

    public void testBothAgentPathAndAgentLibFails() throws Exception {
        delegate.testBothAgentPathAndAgentLibFails();
    }

    public void testFullParse() throws Exception {
        delegate.testFullParse();
    }

    public void testMissingName() throws Exception {
        delegate.testMissingName();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.base.DomainModelElementTestBase#testSerializationDeserialization()
     */
    @Override
    public void testSerializationDeserialization() throws Exception {
        delegate.testSerializationDeserialization();
    }

    public void testPermgenInvalidAttribute() throws Exception {
        delegate.testPermgenInvalidAttribute();
    }

    public void testPermgenMissingMax() throws Exception {
        delegate.testPermgenMissingMax();
    }

    public void testPermgenMissingSize() throws Exception {
        delegate.testPermgenMissingSize();
    }

    public void testHeapInvalidAttribute() throws Exception {
        delegate.testHeapInvalidAttribute();
    }

    public void testHeapMissingMax() throws Exception {
        delegate.testHeapMissingMax();
    }

    public void testHeapMissingSize() throws Exception {
        delegate.testHeapMissingSize();
    }

    public void testJvmInvalidAtribute() throws Exception {
        delegate.testJvmInvalidAtribute();
    }

    public void testStackInvalidAttribute() throws Exception {
        delegate.testStackInvalidAttribute();
    }

    public void testStackMissingSize() throws Exception {
        delegate.testStackMissingSize();
    }

    public void testAgentLibInvalidAttribute() throws Exception {
        delegate.testAgentLibInvalidAttribute();
    }

    public void testAgentLibMissingValue() throws Exception {
        delegate.testAgentLibMissingValue();
    }

    public void testAgentPathInvalidAttribute() throws Exception {
        delegate.testAgentPathInvalidAttribute();
    }

    public void testAgentPathMissingValue() throws Exception {
        delegate.testAgentPathMissingValue();
    }

    public void testJavaagentInvalidAttribute() throws Exception {
        delegate.testJavaagentInvalidAttribute();
    }

    public void testJavaagentMissingValue() throws Exception {
        delegate.testJavaagentMissingValue();
    }
}
