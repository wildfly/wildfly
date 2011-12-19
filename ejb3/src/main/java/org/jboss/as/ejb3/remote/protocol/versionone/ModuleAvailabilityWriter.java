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

package org.jboss.as.ejb3.remote.protocol.versionone;

import java.io.DataOutput;
import java.io.IOException;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.ejb.client.remoting.PackedInteger;

/**
 * @author Jaikiran Pai
 */
class ModuleAvailabilityWriter {

    static final byte HEADER_MODULE_AVAILABLE = 0x08;
    static final byte HEADER_MODULE_UNAVAILABLE = 0x09;

    ModuleAvailabilityWriter() {
    }

    void writeModuleAvailability(final DataOutput output, final DeploymentModuleIdentifier[] availableModules) throws IOException {

        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null output");
        }
        if (availableModules == null) {
            throw new IllegalArgumentException("EJB module identifiers cannot be null");
        }
        // write the header
        output.write(HEADER_MODULE_AVAILABLE);
        // write the module inventory
        this.writeModuleReport(output, availableModules);
    }

    void writeModuleUnAvailability(final DataOutput output, final DeploymentModuleIdentifier[] unavailableModules) throws IOException {

        if (output == null) {
            throw new IllegalArgumentException("Cannot write to null output");
        }
        if (unavailableModules == null) {
            throw new IllegalArgumentException("EJB module identifiers cannot be null");
        }
        // write the header
        output.write(HEADER_MODULE_UNAVAILABLE);
        // write the module inventory
        this.writeModuleReport(output, unavailableModules);
    }

    private void writeModuleReport(final DataOutput output, final DeploymentModuleIdentifier[] modules) throws IOException {
        // write the count
        PackedInteger.writePackedInteger(output, modules.length);
        // write the module identifiers
        for (int i = 0; i < modules.length; i++) {
            // write the app name
            final String appName = modules[i].getApplicationName();
            if (appName == null) {
                // write out a empty string
                output.writeUTF("");
            } else {
                output.writeUTF(appName);
            }
            // write the module name
            output.writeUTF(modules[i].getModuleName());
            // write the distinct name
            final String distinctName = modules[i].getDistinctName();
            if (distinctName == null) {
                // write out an empty string
                output.writeUTF("");
            } else {
                output.writeUTF(distinctName);
            }
        }
    }
}
