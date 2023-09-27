/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.testsuite.integration.secman.propertypermission;

import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;

/**
 * Server setup task, which adds a custom system property to AS configuration.
 *
 * @author Josef Cacek
 */
public class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

    public static final String PROPERTY_NAME = "custom-test-property";

    @Override
    protected SystemProperty[] getSystemProperties() {
        return new SystemProperty[] { new DefaultSystemProperty(PROPERTY_NAME, PROPERTY_NAME) };
    }
}