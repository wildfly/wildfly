/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.ear;

/**
 * @author Stuart Douglas
 */
public class EarLibClassLoadingClass {

    public static void loadClass(final String className) throws ClassNotFoundException {
        EarLibClassLoadingClass.class.getClassLoader().loadClass(className);
    }

}
