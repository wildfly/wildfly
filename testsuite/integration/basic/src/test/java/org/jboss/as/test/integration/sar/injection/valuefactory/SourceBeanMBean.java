/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.sar.injection.valuefactory;

public interface SourceBeanMBean {
    int getCount();

    int getCount(String count);
}
