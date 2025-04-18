/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import io.undertow.server.HandlerWrapper;

/**
 * @author Stuart Douglas
 */
public interface UndertowFilter extends HandlerWrapper {

    int getPriority();

}
