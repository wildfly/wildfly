/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.managedbean.managedproperty;

import static jakarta.faces.annotation.FacesConfig.Version;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.annotation.FacesConfig;

/**
 * TODO remove once standard WildFly moves to Faces 4
 */
@FacesConfig(
        // Activates CDI build-in beans that provide the injection this project
        version = Version.JSF_2_3
)
@ApplicationScoped
public class JSF23ConfigurationBean {
}
