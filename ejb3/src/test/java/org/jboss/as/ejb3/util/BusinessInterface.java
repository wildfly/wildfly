/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.util;

public interface BusinessInterface {

    void businessMethod(String argumentOne, int argumentTwo);

    void businessMethod(String argumentOne, int argumentTwo, boolean argumentThree);
}
