package org.wildfly.extension.opentelemetry.extension;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * This is the bare bones test example that tests subsystem
 * It does same things that {@link SubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at  {@link SubsystemParsingTestCase}
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME, new OpenTelemetrySubsystemExtension());
    }


    @Override
    protected String getSubsystemXml() {
        return "<subsystem xmlns=\"" + OpenTelemetryParser_1_0.NAMESPACE + "\">" +
                "</subsystem>";
    }

}
