/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.el.expressly;

import jakarta.el.MethodExpression;

import org.glassfish.expressly.MethodExpressionLiteral;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.el.MethodExpressionFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(MethodExpressionFactory.class)
public class ExpresslyMethodExpressionFactory implements MethodExpressionFactory {

    @Override
    public MethodExpression createMethodExpression(String name, Class<?> type, Class<?>[] parameters) {
        return new MethodExpressionLiteral(name, type, parameters);
    }
}
