/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
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

    public SubsystemParsingTestCase(OpenTelemetrySubsystemSchema schema) {
        super(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME, new OpenTelemetrySubsystemExtension(), schema,
                OpenTelemetrySubsystemSchema.CURRENT);
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
        Map<String, String> values = new HashMap<>();
        values.put("service-name", "test-service");
        values.put("exporter-type", "otlp");
        values.put("endpoint", "http://localhost:4317");
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
            Assert.assertEquals(node.getType(), ModelType.EXPRESSION);
            Assert.assertEquals("${test." + key + ":" + value + "}", node.asString());
            Assert.assertEquals(value, node.asExpression().resolveString());
        }
    }
}
