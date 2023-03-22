/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.lra;

import org.jboss.as.test.shared.ManagementServerSetupTask;

public class EnableLRAExtensionsSetupTask extends ManagementServerSetupTask {
    private static final String MODULE_LRA_PARTICIPANT = "org.wildfly.extension.microprofile.lra-participant";
    private static final String MODULE_LRA_COORDINATOR = "org.wildfly.extension.microprofile.lra-coordinator";
    private static final String SUBSYSTEM_LRA_PARTICIPANT = "microprofile-lra-participant";
    private static final String SUBSYSTEM_LRA_COORDINATOR = "microprofile-lra-coordinator";

    public EnableLRAExtensionsSetupTask() {
        super(createContainerConfigurationBuilder()
            .setupScript(createScriptBuilder()
                .startBatch()
                .add("/extension=" + MODULE_LRA_COORDINATOR + ":add")
                .add("/subsystem=" + SUBSYSTEM_LRA_COORDINATOR + ":add")
                .add("/extension=" + MODULE_LRA_PARTICIPANT + ":add")
                .add("/subsystem=" + SUBSYSTEM_LRA_PARTICIPANT + ":add")
                .endBatch()
                .build()
            )
            .tearDownScript(createScriptBuilder()
                .startBatch()
                .add("/subsystem=" + SUBSYSTEM_LRA_PARTICIPANT + ":remove")
                .add("/extension=" + MODULE_LRA_PARTICIPANT + ":remove")
                .add("/subsystem=" + SUBSYSTEM_LRA_COORDINATOR + ":remove")
                .add("/extension=" + MODULE_LRA_COORDINATOR + ":remove")
                .endBatch()
                .build())
            .build());
    }
}