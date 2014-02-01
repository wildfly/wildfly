package com.redhat.gss.extension;


import org.jboss.as.controller.RunningMode;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;

import com.redhat.gss.extension.Namespace;
import com.redhat.gss.extension.RedhatAccessPluginEapExtension;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;


public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(RedhatAccessPluginEapExtension.SUBSYSTEM_NAME, new RedhatAccessPluginEapExtension());
    }

    @Test(expected = XMLStreamException.class)
    public void testParseSubsystemWithBadChild() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\"><invalid/></subsystem>";
        super.parse(subsystemXml);
    }

    @Test(expected = XMLStreamException.class)
    public void testParseSubsystemWithBadAttribute() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\" attr=\"wrong\"/>";
        super.parse(subsystemXml);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
         return new AdditionalInitialization(){
             @Override
             protected RunningMode getRunningMode() {
                 return RunningMode.NORMAL;
             }
         };
    }
}