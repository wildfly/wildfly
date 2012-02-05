package org.jboss.as.server.deployment.scanner;


import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.IOException;

/**
 * @author Tomaz Cerar
 * @created 25.1.12 19:36
 */

public class DeploymentScannerParsingTestCase extends AbstractSubsystemBaseTest {
    private static final String SUBSYSTEM_XML =
            "<subsystem xmlns=\"urn:jboss:domain:deployment-scanner:1.1\">\n" +
            "    <deployment-scanner name=\"myScanner\" path=\"deployments_${custom.system.property:test}\" " +
                   "relative-to=\"jboss.server.base.dir\" scan-enabled=\"true\" scan-interval=\"5000\" " +
                   "auto-deploy-xml=\"true\" deployment-timeout=\"60\"/>\n" +
            "    <deployment-scanner path=\"deployments\" relative-to=\"jboss.server.base.dir\" " +
                   "scan-enabled=\"true\" scan-interval=\"5000\" " +
                   "auto-deploy-xml=\"true\" deployment-timeout=\"30\"/>\n" +
            "</subsystem>";


    public DeploymentScannerParsingTestCase() {
        super(DeploymentScannerExtension.SUBSYSTEM_NAME, new DeploymentScannerExtension());
        System.setProperty("custom.system.property","prop");
    }

    /**
     * Get the subsystem xml as string.
     *
     * @return the subsystem xml
     * @throws java.io.IOException
     */
    @Override
    protected String getSubsystemXml() throws IOException {
        return SUBSYSTEM_XML;
    }
}

