package org.wildfly.extension.cluster;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.IOException;

/**
 * This is the barebone test example that tests subsystem
 * It does same things that {@link SubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at  {@link SubsystemParsingTestCase}
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ClusterSubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public ClusterSubsystemBaseParsingTestCase() {
        super(ClusterExtension.SUBSYSTEM_NAME, new ClusterExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-cluster-test.xml");
    }
}
