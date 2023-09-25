/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.valuefactory;

public class TargetBean implements TargetBeanMBean {
    private int sourceCount;
    private int countWithArgument;

    @Override
    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    @Override
    public int getCountWithArgument() {
        return countWithArgument;
    }

    public void setCountWithArgument(int countWithArgument) {
        this.countWithArgument = countWithArgument;
    }
}
