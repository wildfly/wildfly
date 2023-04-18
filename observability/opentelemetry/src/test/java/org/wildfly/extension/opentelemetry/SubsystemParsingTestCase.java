package org.wildfly.extension.opentelemetry;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

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
import org.jboss.dmr.ModelType;
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
    public static Iterable<OpenTelemetrySubsystemSchema> parameters() {
        return EnumSet.allOf(OpenTelemetrySubsystemSchema.class);
    }

    private final OpenTelemetrySubsystemSchema schema;
    private final String testXml;

    public SubsystemParsingTestCase(OpenTelemetrySubsystemSchema schema) {
        super(OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME, new OpenTelemetrySubsystemExtension());
        this.schema = schema;
        this.testXml = "<subsystem xmlns=\"" + this.schema.getNamespace() + "\"></subsystem>";
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
        Assert.assertEquals(OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME, element.getValue());
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
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME));
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

    @Test
    public void testInvalidExporter() throws Exception {
        Assert.assertThrows(XMLStreamException.class, () -> this.parse(readResource("invalid-exporter.xml")));
    }

    @Test
    public void testInvalidSampler() throws Exception {
        Assert.assertThrows(XMLStreamException.class, () -> this.parse(readResource("invalid-sampler.xml")));
    }

    @Test
    public void testInvalidSpanProcessor() throws Exception {
        Assert.assertThrows(XMLStreamException.class, () -> this.parse(readResource("invalid-span-processor.xml")));
    }

    @Test
    public void testExpressions() throws IOException, XMLStreamException {
        String xml = readResource("expressions.xml");
        List<ModelNode> operations = this.parse(xml);

        Assert.assertEquals(1, operations.size());

        //Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        Map<String, String> values = new HashMap<>();
        values.put("service-name", "test-service");
        values.put("exporter-type", "jaeger");
        values.put("endpoint", "http://localhost:14250");
        values.put("span-processor-type", "batch");
        values.put("batch-delay", "5000");
        values.put("max-queue-size", "2048");
        values.put("max-export-batch-size", "512");
        values.put("export-timeout", "30000");
        values.put("sampler-type", "on");
        values.put("ratio", "0.75");

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            ModelNode node = addSubsystem.get(key);
            Assert.assertTrue(node.getType().equals(ModelType.EXPRESSION));
            Assert.assertEquals("${test." + key + ":" + value + "}", node.asString());
            Assert.assertEquals(value, node.asExpression().resolveString());
        }
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format(Locale.ROOT, "schema/wildfly-opentelemetry_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
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
