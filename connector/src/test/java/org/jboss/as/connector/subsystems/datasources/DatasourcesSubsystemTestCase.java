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
package org.jboss.as.connector.subsystems.datasources;

import java.io.IOException;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class DatasourcesSubsystemTestCase extends AbstractSubsystemBaseTest {

    public DatasourcesSubsystemTestCase() {
        super(DataSourcesExtension.SUBSYSTEM_NAME, new DataSourcesExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //test configuration put in standalone.xml
        return readResource("datasources-minimal.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-datasources_6_0.xsd";
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("datasources-full.xml");
    }

    @Test
    public void testElytronConfig() throws Exception {
        standardSubsystemTest("datasources-elytron-enabled_6_0.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-full-expression.xml", "datasources-full.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        // Create a AdditionalInitialization.MANAGEMENT variant that has all the external
        // capabilities used by the various configs used in this test class
        return AdditionalInitialization.MANAGEMENT.withCapabilities(
                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY + ".DsAuthCtxt",
                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY + ".CredentialAuthCtxt",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".test-store",
                NamingService.CAPABILITY_NAME,
                ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME
        );
    }
}

