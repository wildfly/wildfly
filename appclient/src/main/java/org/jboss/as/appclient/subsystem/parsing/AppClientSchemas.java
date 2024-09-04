/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.appclient.subsystem.parsing;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.parsing.ManagementSchemas;
import org.jboss.as.version.Stability;
import org.jboss.modules.ModuleLoader;

/**
 * Representation of the schemas for the application client configuration.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AppClientSchemas extends ManagementSchemas {

    public AppClientSchemas(final Stability stability, final ModuleLoader loader, final ExtensionRegistry extensionRegistry) {
        super(stability, new AppClientXml(loader, extensionRegistry), SERVER);
    }

}
