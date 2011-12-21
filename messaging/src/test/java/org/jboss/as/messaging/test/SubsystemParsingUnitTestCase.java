/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.messaging.test;

import java.io.IOException;

import org.jboss.as.controller.OperationContext.Type;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class SubsystemParsingUnitTestCase extends AbstractSubsystemBaseTest {

    public SubsystemParsingUnitTestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Test
    public void testXsd10() throws Exception {
        standardSubsystemTest("xsd10.xml");
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }


    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new MessagingAdditionalInitialization(Type.MANAGEMENT);
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        if (configId != null && configId.equals("xsd10.xml")) {
            return;
        }

        super.compareXml(configId, original, marshalled, true);
    }
}
