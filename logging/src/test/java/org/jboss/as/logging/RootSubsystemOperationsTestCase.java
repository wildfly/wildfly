/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class RootSubsystemOperationsTestCase extends AbstractOperationsTestCase {

    @Before
    public void clearLogDir() {
        final File dir = LoggingTestEnvironment.get().getLogDir();
        deleteRecursively(dir);
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return LoggingTestEnvironment.get();
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/simple-subsystem.xml");
    }

    @Test
    public void testAttributes() throws Exception {
        final KernelServices kernelServices = boot();
        final ModelNode address = SUBSYSTEM_ADDRESS.toModelNode();
        testWrite(kernelServices, address, LoggingRootResource.ADD_LOGGING_API_DEPENDENCIES, true);
        testUndefine(kernelServices, address, LoggingRootResource.ADD_LOGGING_API_DEPENDENCIES);
    }

    static void deleteRecursively(final File dir) {
        if (dir.isDirectory()) {
            final File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteRecursively(file);
                }
                file.delete();
            }
        }
    }
}
