/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb;

/**
 * User: jpai
 */
public interface EJBBusinessInterface {

    String echo(String msg);

    void loadClass(String className) throws ClassNotFoundException;

}
