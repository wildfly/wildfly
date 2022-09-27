package org.wildfly.extension.micrometer;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.micrometer.model.MicrometerSchema;


/**
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class SubsystemParsingTestCase extends AbstractSubsystemBaseTest {

    public static final String SUBSYSTEM_XML = "<subsystem xmlns=\"" + MicrometerSchema.CURRENT.getNamespaceUri() +
            "\"></subsystem>";
    private String testXml;

    public SubsystemParsingTestCase() {
        super(MicrometerSubsystemExtension.SUBSYSTEM_NAME, new MicrometerSubsystemExtension());
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        List<ModelNode> operations = super.parse(SUBSYSTEM_XML);

        ///Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(MicrometerSubsystemExtension.SUBSYSTEM_NAME, element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        KernelServices services = super.createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(SUBSYSTEM_XML)
                .build();
        System.out.println(services.getBootError());
        Assert.assertTrue(services.isSuccessfulBoot());

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(MicrometerSubsystemExtension.SUBSYSTEM_NAME));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second
     * controller started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices servicesA = super.createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(SUBSYSTEM_XML)
                .build();
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(marshalled)
                .build();
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    @Override
    public void testSchema() throws Exception {
        super.testSchema();
    }

    /**
     * Tests that the subsystem can be removed
     */
    @Test
    public void testSubsystemRemoval() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices services = super.createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(SUBSYSTEM_XML)
                .build();
        //Checks that the subsystem was removed from the model
        super.assertRemoveSubsystemResources(services);

        //TODO Chek that any services that were installed were removed here
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-micrometer_1_0.xsd";
    }

    @Override
    protected String getSubsystemXml() {
        // An ugly hack to get around visibility in the base class. Changing the base class would be preferable, but...
        if (testXml != null) {
            String xml = testXml;
            testXml = null;
            return xml;
        }
        return SUBSYSTEM_XML;
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {

        return new AdditionalInitialization() {

            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }

            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry,
                                                            Resource rootResource,
                                                            ManagementResourceRegistration rootRegistration,
                                                            RuntimeCapabilityRegistry capabilityRegistry) {
            }
        };

    }
}
