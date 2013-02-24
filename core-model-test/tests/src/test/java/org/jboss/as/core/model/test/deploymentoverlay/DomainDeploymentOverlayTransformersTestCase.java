/*
* JBoss, Home of Professional Open Source.
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
package org.jboss.as.core.model.test.deploymentoverlay;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer.TestControllerVersion;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.StandardServerGroupInitializers;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.host.controller.ignored.IgnoreDomainResourceTypeResource;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Parameterized.class)
public class DomainDeploymentOverlayTransformersTestCase extends AbstractCoreModelTest {

    private final ModelVersion modelVersion;
    private final TestControllerVersion testControllerVersion;

    @Parameters
    public static List<Object[]> parameters(){
        return TransformersTestParameters.setupVersions();
    }

    public DomainDeploymentOverlayTransformersTestCase(TransformersTestParameters params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @Test
    public void testDeploymentOverlaysTransformer() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER)
                .createContentRepositoryContent("12345678901234567890")
                .setXmlResource("domain.xml");

        LegacyKernelServicesInitializer legacyInitializer = StandardServerGroupInitializers.addServerGroupInitializers(builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion));

        if (modelVersion.getMajor() == 1 && modelVersion.getMinor() < 4) {
            testDeploymentOverlaysTransformer_7_1_x(modelVersion, builder, legacyInitializer);
        } else {
            testDeploymentOverlaysTransformerMaster(modelVersion, builder, legacyInitializer);
        }
    }

    @Test
    public void testDeploymentOverlaysNotIgnoredOnOlderVersionFails() throws Exception {
        if (modelVersion.getMajor() > 1 || modelVersion.getMinor() >= 4) {
            return;
        }

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER)
                .createContentRepositoryContent("12345678901234567890")
                .setXmlResource("domain.xml");

        //Start up an empty legacy controller
        StandardServerGroupInitializers.addServerGroupInitializers(
                    builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion)
                )
                .setDontUseBootOperations()
                //Since the legacy controller does not know about deployment overlays, there will be not boot ops for the reverse check
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        //Since we had not set up the ignores the legacy controller cannot have the master model applied
        try {
            mainServices.applyMasterDomainModel(modelVersion, null);
            Assert.fail("Should have failed to apply model without deployment-overlay ignored on host");
        } catch(Exception e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("14738")); //14738 comes from ControllerMessages.noChildType(String)
        }
    }

    @Test
    public void testDeploymentOverlaysIgnoredOnOlderVersionGetIgnoredButFailDueToServerGroupEntry() throws Exception {
        if (modelVersion.getMajor() > 1 || modelVersion.getMinor() >= 4) {
            return;
        }

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER)
                .createContentRepositoryContent("12345678901234567890")
                .setXmlResource("domain.xml");

        //Start up an empty legacy controller
        StandardServerGroupInitializers.addServerGroupInitializers(
                    builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion)
                )
                .setDontUseBootOperations()
                //Since the legacy controller does not know about deployment overlays, there will be not boot ops for the reverse check
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        //Since the server group deployment deployment-overlay is there we should fail
        try {
            mainServices.applyMasterDomainModel(modelVersion, null);
            Assert.fail("Should have failed to apply model since server group still has a deployment overlay");
        } catch(Exception e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("14738")); //14738 comes from ControllerMessages.noChildType(String)
        }
    }

    @Test
    public void testDeploymentOverlaysIgnoredOnOlderVersionGetIgnored() throws Exception {
        if (modelVersion.getMajor() > 1 || modelVersion.getMinor() >= 4) {
            return;
        }

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER)
                .createContentRepositoryContent("12345678901234567890")
                .setXmlResource("domain-no-servergroup-overlay.xml");

        //Start up an empty legacy controller
        StandardServerGroupInitializers.addServerGroupInitializers(
                    builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion)
                )
                .setDontUseBootOperations()
                //Since the legacy controller does not know about deployment overlays, there will be not boot ops for the reverse check
                .skipReverseControllerCheck();


        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        //This should pass since the deployment-overlay resource is ignored, and there is no use of deployment-overlay in server-group
        mainServices.applyMasterDomainModel(modelVersion, Collections.singletonList(new IgnoreDomainResourceTypeResource(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, new ModelNode(), true)));

        //Check deployment overlays exist in the master model but not in the legacy model
        ModelNode masterModel = mainServices.readWholeModel();
        ModelNode legacyModel = legacyServices.readWholeModel();
        Assert.assertTrue(masterModel.hasDefined(ModelDescriptionConstants.DEPLOYMENT_OVERLAY) && masterModel.get(ModelDescriptionConstants.DEPLOYMENT_OVERLAY).hasDefined("test-overlay"));
        Assert.assertFalse(legacyModel.hasDefined(ModelDescriptionConstants.DEPLOYMENT_OVERLAY));

        //Compare the transformed and legacy models
        checkCoreModelTransformation(
                mainServices,
                modelVersion,
                new ModelFixer() {
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        //This one is just noise due to a different format
                        //Perhaps this should go into the model comparison itself?
                        ModelNode socketBindingGroup = modelNode.get(SOCKET_BINDING_GROUP, "test-sockets");
                        if (socketBindingGroup.isDefined()) {
                            Set<String> names = new HashSet<String>();
                            for (String key : socketBindingGroup.keys()) {
                                if (!socketBindingGroup.get(key).isDefined()) {
                                    names.add(key);
                                }
                            }
                            for(String name : names) {
                                socketBindingGroup.remove(name);
                            }
                            if (socketBindingGroup.keys().size() == 0) {
                                socketBindingGroup.clear();
                            }
                        }
                        return modelNode;
                    }
                },
                new ModelFixer() {
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        modelNode.remove(ModelDescriptionConstants.DEPLOYMENT_OVERLAY);
                        return modelNode;
                    }
                });
    }
    private void testDeploymentOverlaysTransformer_7_1_x(ModelVersion modelVersion, KernelServicesBuilder builder, LegacyKernelServicesInitializer legacyInitializer) throws Exception {
        //Don't validate ops since they definitely won't exist in target controller
        legacyInitializer.setDontValidateOperations();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertFalse(legacyServices.isSuccessfulBoot());

    }

    private void testDeploymentOverlaysTransformerMaster(ModelVersion modelVersion, KernelServicesBuilder builder, LegacyKernelServicesInitializer legacyInitializer) throws Exception {
        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkCoreModelTransformation(mainServices, modelVersion, MODEL_FIXER, MODEL_FIXER);

    }

    private final ModelFixer MODEL_FIXER = new ModelFixer() {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            modelNode.remove(SOCKET_BINDING_GROUP);
            modelNode.remove(PROFILE);
            return modelNode;
        }
    };

}
