/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
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
public class SubsystemParsingTestCase extends AbstractSubsystemSchemaTest<OpenTelemetrySubsystemSchema> {
    @Parameters
    public static Iterable<OpenTelemetrySubsystemSchema> parameters() {
        return EnumSet.allOf(OpenTelemetrySubsystemSchema.class);
    }

    public SubsystemParsingTestCase(OpenTelemetrySubsystemSchema testSchema) {
        super(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME, new OpenTelemetrySubsystemExtension(),
            testSchema, OpenTelemetrySubsystemSchema.VERSION_2_0_PREVIEW);

        System.err.println("Testing schema: " + testSchema.getNamespace());
    }

    @Test
    public void testInvalidExporter() {
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
        Map<String, String> values = Map.of(
            "service-name", "test-service",
            "exporter-type", "otlp",
            "endpoint", "http://localhost:4317",
            "span-processor-type", "batch",
            "batch-delay", "5000",
            "max-queue-size", "2048",
            "max-export-batch-size", "512",
            "export-timeout", "30000",
            "sampler-type", "on",
            "ratio", "0.75"
        );

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            ModelNode node = addSubsystem.get(key);
            Assert.assertEquals(ModelType.EXPRESSION, node.getType());
            Assert.assertEquals("${test." + key + ":" + value + "}", node.asString());
            Assert.assertEquals(value, node.asExpression().resolveString());
        }
    }
}
