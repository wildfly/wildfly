/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.logging;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.BeforeClass;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LoggingSubsystemTestCase extends AbstractSubsystemBaseTest {

    @BeforeClass
    public static void setUp() {
        // Just need to set-up the test environment
        LoggingTestEnvironment.get();
    }

    public LoggingSubsystemTestCase() {
        super(LoggingExtension.SUBSYSTEM_NAME, new LoggingExtension(), RemoveOperationComparator.INSTANCE);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/logging.xml");
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return LoggingTestEnvironment.get();
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    static class RemoveOperationComparator implements Comparator<PathAddress> {
        static final RemoveOperationComparator INSTANCE = new RemoveOperationComparator();
        static final int GREATER = 1;
        static final int LESS = -1;

        @Override
        public int compare(final PathAddress o1, final PathAddress o2) {
            final String key1 = o1.getLastElement().getKey();
            final String key2 = o2.getLastElement().getKey();
            int result = key1.compareTo(key2);
            if (result != 0) {
                if ("subsystem".equals(key1)) {
                    result = LESS;
                } else if ("subsystem".equals(key2)) {
                    result = GREATER;
                } else if (CommonAttributes.ROOT_LOGGER.equals(key1)) {
                    result = GREATER;
                } else if (CommonAttributes.ROOT_LOGGER.equals(key2)) {
                    result = LESS;
                } else if (CommonAttributes.LOGGER.equals(key1)) {
                    result = GREATER;
                } else if (CommonAttributes.LOGGER.equals(key2)) {
                    result = LESS;
                } else if (CommonAttributes.ASYNC_HANDLER.equals(key1)) {
                    result = GREATER;
                } else if (CommonAttributes.ASYNC_HANDLER.equals(key2)) {
                    result = LESS;
                }
            }
            return result;
        }
    }
}
