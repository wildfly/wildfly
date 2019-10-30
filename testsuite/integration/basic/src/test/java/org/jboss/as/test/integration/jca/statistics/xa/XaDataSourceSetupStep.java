/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.statistics.xa;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;

/**
 * Adds xa-data-source before deployment and removes it at the end.
 *
 * @author dsimko@redhat.com
 *
 */
class XaDataSourceSetupStep extends SnapshotRestoreSetupTask {

    public static final String XA_DATASOURCE_NAME = "ExampleXADS";
    private CLIWrapper cli;

    @Override
    public void doSetup(ManagementClient managementClient, String serverId) throws Exception {
        initCli();
        addXaDatasource(XA_DATASOURCE_NAME);
    }

    @Override
    protected void nonManagementCleanUp() throws Exception {
        quitCli();
    }

    private void addXaDatasource(String name) {
        StringBuilder builder = new StringBuilder();
        builder.append("xa-data-source add --name=");
        builder.append(name);
        builder.append(" --jndi-name=java:jboss/datasources/");
        builder.append(name);
        builder.append(" --driver-name=h2 --user-name=sa --password=sa --statistics-enabled=true --enabled=true");
        builder.append(" --xa-datasource-properties={\"URL\"=>\"jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\"}");
        cli.sendLine(builder.toString());
    }
    private void initCli() throws Exception {
        if (cli == null) {
            cli = new CLIWrapper(true);
        }
    }

    private void quitCli() throws Exception {
        try {
            if (cli != null)
                cli.quit();
        } finally {
            cli = null;
        }
    }

}