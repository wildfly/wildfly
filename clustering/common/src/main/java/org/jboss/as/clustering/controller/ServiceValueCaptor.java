/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Consumer;

import org.wildfly.clustering.service.ServiceNameProvider;

/**
 * Captures the value of a service.
 * @author Paul Ferraro
 */
public interface ServiceValueCaptor<T> extends ServiceNameProvider, Consumer<T> {

}
