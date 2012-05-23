package org.jboss.as.capedwarf.extension;


import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.IOException;

/**
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author Tomaz Cerar
 */
public class SubsystemParsingTestCase extends AbstractSubsystemBaseTest {
    private static final String SUBSYSTEM_XML =
            "<subsystem xmlns=\"urn:jboss:domain:capedwarf:1.0\">\n" +
                    "            <appengine-api>abc123</appengine-api>\n" +
                    "         </subsystem>";

    public SubsystemParsingTestCase() {
        super(CapedwarfExtension.SUBSYSTEM_NAME, new CapedwarfExtension());
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
