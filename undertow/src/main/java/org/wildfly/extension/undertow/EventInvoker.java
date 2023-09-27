/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

/**
 * Implementation of this class knows how to invoke an event on the {@link UndertowEventListener}.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public interface EventInvoker {

    void invoke(UndertowEventListener listener);
}