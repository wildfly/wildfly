package org.jboss.as.undertow.extension;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.undertow.UndertowExtension;

/**
 * This is the barebone test example that tests subsystem
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class UndertowSubsystemTestCase extends AbstractSubsystemBaseTest {

    public UndertowSubsystemTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-1.0.xml");
    }

}
