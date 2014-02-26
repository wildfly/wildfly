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

import java.io.File;
import java.io.IOException;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FormatterOperationsTestCase extends AbstractOperationsTestCase {
    private static final String FQCN = FormatterOperationsTestCase.class.getName();

    private static File logDir;

    @BeforeClass
    public static void setupLoggingDir() {
        logDir = LoggingTestEnvironment.get().getLogDir();
        final File[] files = logDir.listFiles();
        for (File file : (files == null ? new File[0] : files)) {
            file.delete();
        }
    }

    @After
    @Override
    public void clearLogContext() {
        super.clearLogContext();
        final LoggingProfileContextSelector contextSelector = LoggingProfileContextSelector.getInstance();
        if (contextSelector.exists(PROFILE)) {
            clearLogContext(contextSelector.get(PROFILE));
            contextSelector.remove(PROFILE);
        }
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/empty-subsystem.xml");
    }

    @Test
    public void testDefaultOperations() throws Exception {
        final KernelServices kernelServices = boot();

        testPatternFormatter(kernelServices, null);
        testPatternFormatter(kernelServices, PROFILE);
    }

    private void testPatternFormatter(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createPatternFormatterAddress(profileName, "PATTERN").toModelNode();

        // Add the pattern formatter
        final ModelNode addOp = SubsystemOperations.createAddOperation(address);
        executeOperation(kernelServices, addOp);

        // Write each attribute and check the value
        testWrite(kernelServices, address, PatternFormatterResourceDefinition.PATTERN, "[test] %d{HH:mm:ss,SSS} %-5p [%c] %s%E%n");
        testWrite(kernelServices, address, PatternFormatterResourceDefinition.COLOR_MAP, "info:blue,warn:yellow,error:red,debug:cyan");

        // Undefine attributes
        testUndefine(kernelServices, address, PatternFormatterResourceDefinition.PATTERN);
        testUndefine(kernelServices, address, PatternFormatterResourceDefinition.COLOR_MAP);

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
    }
}
