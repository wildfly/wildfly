/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.security.manager;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * Security Manager subsystem tests.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
public class SecurityManagerSubsystemTestCase extends AbstractSubsystemBaseTest {

    public SecurityManagerSubsystemTestCase() {
        super(Constants.SUBSYSTEM_NAME, new SecurityManagerExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("security-manager-1.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-security-manager_1_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/security-manager.xml"
        };
    }
}
