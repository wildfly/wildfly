/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.jpa.hibernate.transformer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Enable Hibernate bytecode transformer globally with system-property Hibernate51CompatibilityTransformer=true
 */
@RunWith(Arquillian.class)
@ServerSetup(VerifyHibernate51CompatibilityPropertyEnabledTransformerTestCase.EnableHibernateBytecodeTransformerSetupTask.class)
public class VerifyHibernate51CompatibilityPropertyEnabledTransformerTestCase
        extends AbstractVerifyHibernate51CompatibilityTestCase {

    @Deployment
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                VerifyHibernate51CompatibilityPropertyEnabledTransformerTestCase.class.getName() + ".ear");
        ear.addAsLibraries(getLib());

        WebArchive war = getWar();
        war.addClasses(VerifyHibernate51CompatibilityPropertyEnabledTransformerTestCase.class);
        ear.addAsModule(war);

        ear.addAsManifestResource(new StringAsset("<jboss-deployment-structure>" + " <deployment>" + " <dependencies>"
                + " <module name=\"org.hibernate\" export=\"true\" />"
                + " <module name=\"com.h2database.h2\" />" + " <module name=\"org.slf4j\"/>" + " </dependencies>"
                + " </deployment>"
                + "</jboss-deployment-structure>"), "jboss-deployment-structure.xml");
        return ear;
    }

    public static class EnableHibernateBytecodeTransformerSetupTask implements ServerSetupTask {
        private static final ModelNode PROP_ADDR = new ModelNode()
                .add("system-property", "Hibernate51CompatibilityTransformer");

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode op = Operations.createAddOperation(PROP_ADDR);
            op.get("value").set("true");
            managementClient.getControllerClient().execute(op);
            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            ModelNode op = Operations.createRemoveOperation(PROP_ADDR);
            managementClient.getControllerClient().execute(op);
            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }
    }
}
