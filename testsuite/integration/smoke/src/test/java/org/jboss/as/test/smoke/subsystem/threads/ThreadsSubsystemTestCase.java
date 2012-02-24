/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.subsystem.threads;

import java.io.IOException;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.threads.ThreadsExtension;

/**
 * The standard subsystem test, applied to the threads subsystem. This would normally be in the threads subsystem
 * but that would lead to circular dependencies.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ThreadsSubsystemTestCase extends AbstractSubsystemBaseTest {

    public ThreadsSubsystemTestCase() {
        super(ThreadsExtension.SUBSYSTEM_NAME, new ThreadsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return getSubsystemXml("subsystem.xml");
    }

    @Override
    protected void assertRemoveSubsystemResources(KernelServices kernelServices, Set<PathAddress> ignoredChildAddresses) {
        // Disable this, as it does not work in the testsuite. The woodstox XMLStreamWriter that gets picked up in the testsuite
        // fails when the model has no resources and nothing gets written. Seems to work in the subsystem modules,
        // but I (Brian Stansberry) that a JDK writer is used, not woodstox
    }
}
