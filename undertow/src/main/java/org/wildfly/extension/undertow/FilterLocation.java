/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

/**
 * @author Stuart Douglas
 */
public interface FilterLocation {

    void addFilter(UndertowFilter filterRef);

    void removeFilter(UndertowFilter filterRef);

}
