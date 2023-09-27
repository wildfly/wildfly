/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.ejb.stateless;


import jakarta.ejb.Stateless;


/**
 * @author baranowb
 */
@Stateless(name="POINT")
public class PointLessMathBean implements PointlessMathInterface {

    @Override
    public double pointlesMathOperation(double a, double b, double c) {
        return b * b - 4 * a * c;
    }
}
