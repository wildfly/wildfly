/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.bridgemethods;

/**
 * @author Stuart Douglas
 */
public interface GenericInterface {

    Object method(boolean intercepted);
    Object cdiMethod(boolean intercepted);

}
