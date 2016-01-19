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
package org.jboss.as.security;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

public class SecurityDomainModelv11UnitTestCase extends AbstractSubsystemBaseTest {

    public SecurityDomainModelv11UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv11.xml");
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-security_1_1.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties p = new Properties();
        p.setProperty("jboss.server.config.dir", "/some/path");
        return p;
    }

    @Test
    public void testParseAndMarshalModelWithJASPI() throws Exception {
        super.standardSubsystemTest("securitysubsystemJASPIv11.xml", false);
    }
}