/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import jakarta.ejb.EJBLocalObject;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface LocalStatefulSession30 extends EJBLocalObject {
    String getLocalValue();

    void setLocalValue(String value);
}
