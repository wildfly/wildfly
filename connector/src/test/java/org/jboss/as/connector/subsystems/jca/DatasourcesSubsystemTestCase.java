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
package org.jboss.as.connector.subsystems.jca;

import java.io.IOException;

import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DatasourcesSubsystemTestCase extends AbstractSubsystemBaseTest {

    public DatasourcesSubsystemTestCase() {
        // FIXME DatasourcesSubsystemTestCase constructor
        super(DataSourcesExtension.SUBSYSTEM_NAME, new DataSourcesExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //TODO: This is copied from standalone.xml you may want to try more combinations
        return  "<subsystem xmlns=\"urn:jboss:domain:datasources:1.0\">" +
                "    <datasources>" +
                "        <datasource jndi-name=\"java:jboss/datasources/ExampleDS\" enabled=\"false\" use-java-context=\"true\" pool-name=\"H2DS\">" +
                "            <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1</connection-url>" +
                "            <driver>h2</driver>" +
                "            <pool></pool>" +
                "            <security>" +
                "                <user-name>sa</user-name>" +
                "                <password>sa</password>" +
                "            </security>" +
                "        </datasource>" +
                "        <drivers>" +
                "            <driver name=\"h2\" module=\"com.h2database.h2\">" +
                "                <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>" +
                "            </driver>" +
                "        </drivers>" +
                "    </datasources>" +
                "</subsystem>";
    }


    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }

        };
    }


    @Override
    protected void validateXml(String configId, String original, String marshalled) throws Exception {
        //FIXME remove this and marshall properly
    }

}
