/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.ejb.statefull;


import jakarta.ejb.Stateful;


/**
 * @author baranowb
 */
@Stateful(name = "POINT")
public class PointLessMathBean implements PointlesMathInterface {

    @Override
    public double pointlesMathOperation(double a, double b, double c) {
        return b * b - 4 * a * c;
    }
}
