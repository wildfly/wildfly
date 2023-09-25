/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.ztatic;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;

/**
 * An EJB that "asks" for the forbidden static field injection.
 *
 * @author Eduardo Martins
 *
 */
@Stateless
public class FieldTestEJB {

    @Resource(name = "simpleString")
    private static String simpleStringFromDeploymentDescriptor;

    public boolean isStaticResourceInjected() {
        return simpleStringFromDeploymentDescriptor != null;
    }
}
