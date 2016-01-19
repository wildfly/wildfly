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

import org.jboss.as.messaging.MessagingExtension;
import org.junit.Test;

/**
 * * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class MessagingSubsystem20TestCase extends AbstractLegacySubsystemBaseTest {

    public MessagingSubsystem20TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        // subsystem_2_0_expressions.xml contains new http-connector resource
        // introduces in 2.0 management version
        return readResource("subsystem_2_0_expressions.xml");
    }

    @Test
    public void testMessageCounterEnabled() throws Exception {
        standardSubsystemTest("subsystem_2_0_message_counter.xml", false);
    }

    protected void compareXml(String configId, final String original, final String marshalled) throws Exception {
        // XML from messaging 2.0 does not have the same output than 3.0
        return;
    }


}
