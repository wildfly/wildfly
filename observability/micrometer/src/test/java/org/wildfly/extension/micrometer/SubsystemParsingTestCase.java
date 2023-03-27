package org.wildfly.extension.micrometer;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
@RunWith(Parameterized.class)
public class SubsystemParsingTestCase extends AbstractSubsystemBaseTest {
    @Parameters
    public static Iterable<MicrometerSubsystemSchema> parameters() {
        return EnumSet.allOf(MicrometerSubsystemSchema.class);
    }

    private final MicrometerSubsystemSchema schema;
    private final String testXml;

    public SubsystemParsingTestCase(MicrometerSubsystemSchema schema) {
        super(MicrometerExtension.SUBSYSTEM_NAME, new MicrometerExtension());
        this.schema = schema;
        this.testXml = "<subsystem xmlns=\"" + this.schema.getNamespace() +
                "\" endpoint=\"http://localhost:4318/v1/metrics\"></subsystem>";
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        List<ModelNode> operations = super.parse(this.testXml);

        ///Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(MicrometerExtension.SUBSYSTEM_NAME, element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
        //Parse the subsystem xml and install into the controller
        KernelServices services = super.createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(this.testXml)
                .build();
        System.out.println(services.getBootError());
        Assert.assertTrue(services.isSuccessfulBoot());

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(MicrometerExtension.SUBSYSTEM_NAME));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second
     * controller started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices servicesA = super.createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(this.testXml)
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
                .setSubsystemXml(this.testXml)
                .build();
        //Checks that the subsystem was removed from the model
        super.assertRemoveSubsystemResources(services);

        //TODO Chek that any services that were installed were removed here
    }

    @Override
    protected String getSubsystemXsdPath() {
        return String.format(Locale.ROOT, "schema/wildfly-micrometer_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }

    @Override
    protected String getSubsystemXml() {
        return this.testXml;
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
