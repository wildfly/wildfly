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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
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
        return "<subsystem xmlns=\"urn:jboss:domain:jsf:1.1\"" +
                " default-jsf-impl-slot=\"${exp.default-jsf-impl-slot:main}\"" +
                " disallow-doctype-decl=\"${exp.disallow-doctype-decl:true}\" />";
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

    @Test
    public void testAttributes() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(null)
                .setSubsystemXml(getSubsystemXml());
        KernelServices kernelServices = builder.build();
        ModelNode rootModel = kernelServices.readWholeModel();
        ModelNode serverModel = rootModel.require(SUBSYSTEM).require(JSFExtension.SUBSYSTEM_NAME);
        assertEquals("main", serverModel.get(JSFResourceDefinition.DEFAULT_SLOT_ATTR_NAME).resolve().asString());
        assertEquals(true, serverModel.get(JSFResourceDefinition.DISALLOW_DOCTYPE_DECL_ATTR_NAME).resolve().asBoolean());
    }
}
