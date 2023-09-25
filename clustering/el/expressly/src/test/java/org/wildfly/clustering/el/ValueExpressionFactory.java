/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.el;

import jakarta.el.ValueExpression;

/**
 * @author Paul Ferraro
 */
public interface ValueExpressionFactory {

    ValueExpression createValueExpression(String name, Class<?> type);
}
