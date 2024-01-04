/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component;

import java.util.Map;

import org.jboss.as.naming.ManagedReference;

/**
 * Factory that can be used to customize a views proxy creation
 *
 * TODO: this needs to be thought through a bit more
 *
 * @author Stuart Douglas
 */
public interface ViewInstanceFactory {

    ManagedReference createViewInstance(ComponentView componentView, final Map<Object, Object> contextData) throws Exception;

}
