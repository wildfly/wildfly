/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.servicexml;

public class ServiceXmlDeploymentService implements ServiceXmlDeploymentServiceMBean {

    @Override
    public int add(int x, int y) {
        return x + y;
    }
}
