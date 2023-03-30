package org.wildfly.extension.micrometer;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
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

    public SubsystemParsingTestCase(MicrometerSubsystemSchema schema) {
        super(MicrometerExtension.SUBSYSTEM_NAME, new MicrometerExtension());
        this.schema = schema;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("micrometer.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return String.format(Locale.ROOT, "schema/wildfly-micrometer_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }
}
