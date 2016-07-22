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
package org.jboss.as.jsf.subsystem;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

import java.io.IOException;

/**
 *
 * @author Stuart Douglas
 */
public class JSFSubsystemTestCase extends AbstractSubsystemBaseTest {
    public JSFSubsystemTestCase() {
        super(JSFExtension.SUBSYSTEM_NAME, new JSFExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"urn:jboss:domain:jsf:1.1\" />";
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jsf_1_1.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/jsf.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }
}
