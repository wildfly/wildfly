/* JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.subsystem;

import java.io.IOException;

import junit.framework.Assert;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.webservices.dmr.Constants;
import org.jboss.as.webservices.dmr.WSExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WebServicesSubsystemTestCase extends AbstractSubsystemBaseTest {

    public WebServicesSubsystemTestCase() {
        super(WSExtension.SUBSYSTEM_NAME, new WSExtension());
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Test
    public void testTransformers_1_1() throws Exception {
        String subsystemXml = getSubsystemXml();
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-webservices-server-integration:7.1.2.Final");

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        ModelNode legacyModel = checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                //The legacy subsystem has this pointless and unused entry.
                //It could also be added with a transformer but fails validation since 'endpoint'
                //does not exist in the legacy subsystems definition
                if (modelNode.hasDefined(Constants.ENDPOINT)) {
                    modelNode.remove(Constants.ENDPOINT);
                }
                return modelNode;
            }
        });
    }

}
