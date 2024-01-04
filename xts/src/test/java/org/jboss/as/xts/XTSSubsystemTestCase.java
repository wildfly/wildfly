/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.xts;

import java.io.IOException;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class XTSSubsystemTestCase extends AbstractSubsystemBaseTest {

    public XTSSubsystemTestCase() {
        super(XTSExtension.SUBSYSTEM_NAME, new XTSExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(String.format("subsystem-%s.xml", XTSExtension.CURRENT_MODEL_VERSION));
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/jboss-as-xts_3_0.xsd";
    }
}
