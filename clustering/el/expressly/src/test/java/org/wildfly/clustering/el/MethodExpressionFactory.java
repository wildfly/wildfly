/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.el;

import jakarta.el.MethodExpression;

/**
 * @author Paul Ferraro
 */
public interface MethodExpressionFactory {

    MethodExpression createMethodExpression(String name, Class<?> type, Class<?>[] parameters);
}
