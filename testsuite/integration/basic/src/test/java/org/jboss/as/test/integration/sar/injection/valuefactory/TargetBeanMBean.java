/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.valuefactory;

public interface TargetBeanMBean {
    int getSourceCount();

    int getCountWithArgument();
}
