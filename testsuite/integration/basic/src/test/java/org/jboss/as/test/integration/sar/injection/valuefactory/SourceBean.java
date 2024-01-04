/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.valuefactory;

public class SourceBean implements SourceBeanMBean {
    private int count;

    @Override
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public int getCount(String count) {
        return Integer.parseInt(count);
    }
}
