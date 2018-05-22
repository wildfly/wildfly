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
package org.jboss.as.test.integration.jca;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerSnapshot;

/**
 * Implementation of ServerSetupTask for JCA related tests
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public abstract class JcaMgmtServerSetupTask extends JcaMgmtBase implements ServerSetupTask {

    private AutoCloseable snapshot;

    @Override
    public final void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        snapshot = ServerSnapshot.takeSnapshot(managementClient);
        setManagementClient(managementClient);
        doSetup(managementClient);
    }

    protected abstract void doSetup(final ManagementClient managementClient) throws Exception;

    @Override
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        snapshot.close();
    }
}
