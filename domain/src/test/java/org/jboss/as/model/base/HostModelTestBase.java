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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.HostModel;
import org.jboss.as.model.ManagementElement;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.base.util.ModelParsingSupport;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class HostModelTestBase extends DomainModelElementTestBase {


    public HostModelTestBase(String name) {
        super(name);
    }

    public void testManagement() throws Exception {
        final String content = "<management interface=\"public\" port=\"9999\"/>";
        final HostModel model = parse(content);
        final ManagementElement element = model.getManagementElement();
        assertNotNull(element);
        assertEquals("public", element.getInterfaceName());
        assertEquals(9999, element.getPort());
    }

    public void testPaths() throws Exception {
        final String content = "<paths><path name=\"dev.null\" path=\"/dev/null\" /><path name=\"relative.to\" path=\"test\" relative-to=\"dev.null\" /></paths>";
        final HostModel model = parse(content);
        assertNotNull(model.getPath("dev.null"));
    }

    public void testRestrictedPaths() throws Exception {
        final String content = "<paths><path name=\"jboss.home.dir\" path=\"/opt/jboss-as7\" /></paths>";
        try {
            parse(content);
            fail("jboss.home.dir");
        } catch(Exception ok) {
            //
        }
    }

    public void testMissingPaths() throws Exception {
        final String content = "<paths><path name=\"no.path\" /></paths>";
        try {
            parse(content);
            fail("no path");
        } catch(Exception ok) {
            //
        }
    }

    /** {@inheritDoc} */
    public void testSerializationDeserialization() throws Exception {
        // TODO Auto-generated method stub
    }

    HostModel parse(final String content) throws XMLStreamException, UpdateFailedException {
        return ModelParsingSupport.parseHostModel(getXMLMapper(), getFullContent(content));
    }

    String getFullContent(final String content) {
        return ModelParsingSupport.getXmlContent("host", getTargetNamespace(), getTargetNamespaceLocation(), content);
    }
}
