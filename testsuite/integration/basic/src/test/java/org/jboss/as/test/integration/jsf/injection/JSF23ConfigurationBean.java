/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.injection;

import static jakarta.faces.annotation.FacesConfig.Version;

import jakarta.faces.annotation.FacesConfig;

/**
 * Configuration bean to specify Jakarta Server Faces 2.3 version.
 *
 * @author rmartinc
 */
@FacesConfig(
        // Activates CDI build-in beans that provide the injection this project
        version = Version.JSF_2_3
)
public class JSF23ConfigurationBean {
}
