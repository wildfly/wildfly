/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi.tck;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.kohsuke.MetaInfServices;

/**
 * Arquillian extension that adds an observer that enhances archives prior to deployment.
 * @author Paul Ferraro
 */
@MetaInfServices(LoadableExtension.class)
public class MicroProfileOpenAPIExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(DeploymentEnhancer.class);
    }
}
