/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

/**
 * Interface to be implemented by metric enumerations.
 * @author Paul Ferraro
 * @param <C> metric context
 */
public interface Metric<C> extends Attribute, Executable<C> {
}
