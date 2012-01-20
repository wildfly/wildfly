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

import junit.framework.Assert;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.controller.OperationContext.Type;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.jboss.as.connector.subsystems.jca.ParseUtils.checkModelParams;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.nonXaDsProperties;
import static org.jboss.as.connector.subsystems.jca.ParseUtils.xaDsProperties;

/**
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
//@Ignore
public class ComplexDatasourceSubsystemTestCase extends AbstractSubsystemTest {

    public ComplexDatasourceSubsystemTestCase() {
        super(DataSourcesExtension.SUBSYSTEM_NAME, new DataSourcesExtension());
    }

    @Test
    public void testDatasource() throws Exception{
        // Only contain the subsystem xml, e.g.
        //  <subsystem xmlns="urn:jboss:domain:datasources:1.0"> ... </subsystem>
        String xml = readResource("datasource.xml");

        KernelServices services = super.installInController(new AdditionalInitialization() {

            @Override
            protected Type getType() {
                //This override makes it only install in the model, not create the services
                return Type.MANAGEMENT;
            }

        }, xml);

        ModelNode model = services.readWholeModel();

        //Check model..
        final String complexDs = "complexDs";
        final String complexDsJndi = "java:jboss/datasources/" + complexDs;
        Properties params=nonXaDsProperties(complexDsJndi);
        ModelNode modelDs=model.get("subsystem", "datasources","data-source",complexDs+"_Pool");
        checkModelParams(modelDs,params);
        Assert.assertEquals(modelDs.asString(),"UTF-8",modelDs.get("connection-properties","char.encoding","value").asString());
        Assert.assertEquals(modelDs.asString(),"Property2",modelDs.get("valid-connection-checker-properties","name").asString());
        Assert.assertEquals(modelDs.asString(),"Property4",modelDs.get("exception-sorter-properties","name").asString());
        Assert.assertEquals(modelDs.asString(),"Property3",modelDs.get("stale-connection-checker-properties","name").asString());
        Assert.assertEquals(modelDs.asString(),"Property1",modelDs.get("reauth-plugin-properties","name").asString());
        
        final String complexXaDs = "complexXaDs";
        final String complexXaDsJndi = "java:jboss/xa-datasources/" + complexXaDs;
        params=xaDsProperties(complexXaDsJndi);
        ModelNode modelXaDs=model.get("subsystem", "datasources","xa-data-source",complexXaDs+"_Pool");
        checkModelParams(modelXaDs,params);
        Assert.assertEquals(modelXaDs.asString(),"jdbc:h2:mem:test",modelXaDs.get("xa-datasource-properties","URL","value").asString());
        Assert.assertEquals(modelXaDs.asString(),"Property2",modelXaDs.get("valid-connection-checker-properties","name").asString());
        Assert.assertEquals(modelXaDs.asString(),"Property4",modelXaDs.get("exception-sorter-properties","name").asString());
        Assert.assertEquals(modelXaDs.asString(),"Property3",modelXaDs.get("stale-connection-checker-properties","name").asString());
        Assert.assertEquals(modelXaDs.asString(),"Property1",modelXaDs.get("reauth-plugin-properties","name").asString());
        Assert.assertEquals(modelXaDs.asString(),"Property5",modelXaDs.get("recovery-plugin-properties","name").asString());
        Assert.assertEquals(modelXaDs.asString(),"Property6",modelXaDs.get("recovery-plugin-properties","name1").asString());

        //Marshal the xml to see that it is the same as before
        String marshalled = services.getPersistedSubsystemXml();
       // Assert.assertEquals(normalizeXML(xml), normalizeXML(marshalled));

        services = super.installInController(new AdditionalInitialization() {

            @Override
            protected Type getType() {
                //This override makes it only install in the model, not create the services
                return Type.MANAGEMENT;
            }

        }, marshalled);

        //Check that the model looks the same
        ModelNode modelReloaded = services.readWholeModel();
        compare(model, modelReloaded);


        assertRemoveSubsystemResources(services);
    }
}
