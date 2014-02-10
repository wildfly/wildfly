package com.redhat.gss.extension;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import com.redhat.gss.extension.RedhatAccessPluginExtension;

import java.io.IOException;

public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(RedhatAccessPluginExtension.SUBSYSTEM_NAME,
                new RedhatAccessPluginExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }
}
