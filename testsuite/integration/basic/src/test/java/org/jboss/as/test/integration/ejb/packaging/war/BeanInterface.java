/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.war;

/**
 * @author Ondrej Chaloupka
 */
public interface BeanInterface {
    String checkMe();

    String say();
}
