package org.wildfly.clustering.monitor.extension;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.wildfly.clustering.monitor.extension.ClusteringMonitorExtension;

/**
 * This is the barebone test example that tests subsystem
 * It does same things that {@link SubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at  {@link SubsystemParsingTestCase}
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ClusteringMonitorSubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public ClusteringMonitorSubsystemBaseParsingTestCase() {
        super(ClusteringMonitorExtension.SUBSYSTEM_NAME, new ClusteringMonitorExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-cluster-test.xml");
    }
}
