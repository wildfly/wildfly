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
package org.jboss.as.jsr77.subsystem;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JSR77ManagementSubsystemTestCase extends AbstractSubsystemBaseTest {

    private static final AdditionalInitialization ADDITIONAL_INITIALIZATION = AdditionalInitialization.withCapabilities(JSR77ManagementRootResource.JMX_CAPABILITY);

    public JSR77ManagementSubsystemTestCase() {
        super(JSR77ManagementExtension.SUBSYSTEM_NAME, new JSR77ManagementExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"urn:jboss:domain:jsr77:1.0\"/>";
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return ADDITIONAL_INITIALIZATION;
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jsr77_1_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/jsr77.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }
}
