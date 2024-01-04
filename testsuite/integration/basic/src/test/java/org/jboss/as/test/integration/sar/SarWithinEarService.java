/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar;

/**
 * User: jpai
 */
public class SarWithinEarService implements SarWithinEarServiceMBean {

    public SarWithinEarService() {

    }


    @Override
    public int add(int x, int y) {
        return x + y;
    }
}
