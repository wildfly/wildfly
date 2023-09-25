/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.ztatic;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;

/**
 * An EJB that "asks" for the forbidden static method injection.
 *
 * @author Eduardo Martins
 *
 */
@Stateless
public class MethodTestEJB {

    private static String simpleStringFromDeploymentDescriptor;

    @Resource(name = "simpleString")
    private static void setSimpleStringFromDeploymentDescriptor(String s) {
        simpleStringFromDeploymentDescriptor = s;
    }

    public boolean isStaticResourceInjected() {
        return simpleStringFromDeploymentDescriptor != null;
    }

}
