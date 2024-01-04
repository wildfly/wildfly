/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.el.expressly;

import jakarta.el.ValueExpression;

import org.glassfish.expressly.ValueExpressionLiteral;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.el.ValueExpressionFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ValueExpressionFactory.class)
public class ExpresslyValueExpressionFactory implements ValueExpressionFactory {

    @Override
    public ValueExpression createValueExpression(String name, Class<?> type) {
        return new ValueExpressionLiteral(name, type);
    }
}
