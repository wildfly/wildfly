package org.jboss.as.insights;

import java.io.IOException;

import org.jboss.as.insights.extension.InsightsExtension;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

public class InsightsSubsystemTestCase extends AbstractSubsystemBaseTest {

    public InsightsSubsystemTestCase() {
        super(InsightsExtension.SUBSYSTEM_NAME, new InsightsExtension());
    }
    
    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("insights-1.0.xml");
    }
    
    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-insights_1_0.xsd";
    }
}
