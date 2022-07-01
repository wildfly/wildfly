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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.server.service.ClusteringDefaultRequirement;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class JcaSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JcaSubsystemTestCase() {
        super(JcaExtension.SUBSYSTEM_NAME, new JcaExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jca.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-jca_6_0.xsd";
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName(),
                ConnectorServices.LOCAL_TRANSACTION_PROVIDER_CAPABILITY,
                ConnectorServices.TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY,
                ConnectorServices.TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY,
                NamingService.CAPABILITY_NAME,
                "org.wildfly.threads.thread-factory.string");
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("jca-full.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("jca-full-expression.xml", "jca-full.xml");
    }

    /** WFLY-2640 and WFLY-8141 */
    @Test
    public void testCCMHandling() throws Exception {
        String xml = readResource("jca-minimal.xml");
        final KernelServices services = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(xml).build();
        assertTrue("Subsystem boot failed!", services.isSuccessfulBoot());
        //Get the model and the persisted xml from the first controller
        final ModelNode model = services.readWholeModel();

        // ccm is present despite not being in xml
        ModelNode ccm = model.get("subsystem", "jca", "cached-connection-manager", "cached-connection-manager");
        assertTrue(ccm.isDefined());
        assertTrue(ccm.hasDefined("install")); // only true because readWholeModel reads defaults

        // Because it exists we can do a write-attribute
        PathAddress ccmAddress = PathAddress.pathAddress("subsystem", "jca").append("cached-connection-manager", "cached-connection-manager");
        ModelNode writeOp = Util.getWriteAttributeOperation(ccmAddress, "install", true);
        services.executeForResult(writeOp);

        ModelNode readOp = Util.getReadAttributeOperation(ccmAddress, "install");
        ModelNode result = services.executeForResult(readOp);
        assertTrue(result.asBoolean());

        ModelNode removeOp = Util.createRemoveOperation(ccmAddress);
        services.executeForResult(removeOp);

        // Read still works despite removal, but now the attributes are back to defaults
        result = services.executeForResult(readOp);
        assertFalse(result.asBoolean());

        // Write attribute works despite removal
        services.executeForResult(writeOp);

        ModelNode addOp = Util.createAddOperation(ccmAddress);
        addOp.get("debug").set(true);
        services.executeForFailure(addOp); // already exists, with install=true

        // Reset to default state and now we can add
        ModelNode undefineOp = Util.createEmptyOperation("undefine-attribute", ccmAddress);
        undefineOp.get("name").set("install");
        services.executeForResult(undefineOp);
        result = services.executeForResult(readOp);
        assertFalse(result.asBoolean());

        // Now add works
        services.executeForResult(addOp);

        ModelNode readOp2 = Util.getReadAttributeOperation(ccmAddress, "debug");
        result = services.executeForResult(readOp2);
        assertTrue(result.asBoolean());

        // Cant' add again
        services.executeForFailure(addOp);

        // Remove and re-add
        services.executeForResult(removeOp);
        result = services.executeForResult(readOp2);
        assertFalse(result.asBoolean());
        services.executeForResult(addOp);
        result = services.executeForResult(readOp2);
        assertTrue(result.asBoolean());
    }

    /** WFLY-11104 */
    @Test
    public void testMultipleWorkManagers() throws Exception {
        String xml = readResource("jca-minimal.xml");
        final KernelServices services = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(xml).build();
        assertTrue("Subsystem boot failed!", services.isSuccessfulBoot());
        //Get the model and the persisted xml from the first controller
        final ModelNode model = services.readWholeModel();

        PathAddress subystem = PathAddress.pathAddress("subsystem", "jca");
        PathAddress dwm1 = subystem.append("distributed-workmanager", "dwm1");
        PathAddress threads1 = dwm1.append("short-running-threads","dwm1");
        PathAddress dwm2 = subystem.append("distributed-workmanager", "dwm2");
        PathAddress threads2 = dwm2.append("short-running-threads","dwm2");

        ModelNode composite = Util.createEmptyOperation("composite", PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get("steps");

        ModelNode addDwm1 = Util.createAddOperation(dwm1);
        addDwm1.get("name").set("dwm1");
        steps.add(addDwm1);

        ModelNode addThreads1 = Util.createAddOperation(threads1);
        addThreads1.get("max-threads").set(11);
        addThreads1.get("queue-length").set(22);
        steps.add(addThreads1);

        ModelNode addDwm2 = Util.createAddOperation(dwm2);
        addDwm2.get("name").set("dwm2");
        steps.add(addDwm2);

        ModelNode addThreads2 = Util.createAddOperation(threads2);
        addThreads2.get("max-threads").set(11);
        addThreads2.get("queue-length").set(22);
        steps.add(addThreads2);

        services.executeForResult(composite);
    }

    /** WFLY-16478. Test transformation of undefined elytron-enabled */
    @Test
    public void testEAP74Transformation() throws Exception {
        ModelTestControllerVersion eap74ControllerVersion = ModelTestControllerVersion.EAP_7_4_0;
        ModelVersion eap74ModelVersion = ModelVersion.create(5, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("jca-default-elytron.xml");
        KernelServices mainServices = initialKernelServices(builder, eap74ControllerVersion, eap74ModelVersion);

        ModelNode legacyModel = checkSubsystemModelTransformation(mainServices, eap74ModelVersion);
        assertTrue(legacyModel.toString(), legacyModel.get("subsystem", "jca", "workmanager", "default", "elytron-enabled").asBoolean(false));
        assertTrue(legacyModel.toString(), legacyModel.get("subsystem", "jca", "workmanager", "anotherWm", "elytron-enabled").asBoolean(false));
        assertTrue(legacyModel.toString(), legacyModel.get("subsystem", "jca", "distributed-workmanager", "MyDWM", "elytron-enabled").asBoolean(false));
        mainServices.shutdown();
    }

    /** WFLY-16478 Test legacy parser sets old default value for elytron-enabled */
    @Test
    public void testLegacyDefaultElytronEnabled() throws Exception {
        Set<ModelNode> required = new HashSet<>();
        PathAddress subsystem = PathAddress.pathAddress("subsystem", "jca");
        required.add(subsystem.append("workmanager", "default").toModelNode());
        required.add(subsystem.append("workmanager", "anotherWm").toModelNode());
        required.add(subsystem.append("distributed-workmanager", "MyDWM").toModelNode());

        for (ModelNode op : parse(getSubsystemXml("jca_5_0.xml"))) {
            if (ADD.equals(op.get(OP).asString()) && required.remove(op.get(ModelDescriptionConstants.OP_ADDR))) {
                assertFalse(op.toString() + "\n did not correctly define elytron-enabled", op.get("elytron-enabled").asBoolean(true));
            }
        }

        assertTrue("Not all expected ops were found\n" + required.toString(), required.isEmpty());
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    private KernelServices initialKernelServices(KernelServicesBuilder builder, ModelTestControllerVersion controllerVersion, final ModelVersion modelVersion) throws Exception {
        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, modelVersion);
        String mavenGroupId = controllerVersion.getMavenGroupId();
        String artifactId = "wildfly-connector";
        initializer.addMavenResourceURL(mavenGroupId + ":" + artifactId + ":" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL(mavenGroupId + ":wildfly-clustering-api:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL(mavenGroupId + ":wildfly-clustering-spi:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.wildfly.core:wildfly-threads:" + controllerVersion.getCoreVersion())
                .setExtensionClassName("org.jboss.as.connector.subsystems.jca.JcaExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));
        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);
        return mainServices;
    }


}
