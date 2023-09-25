/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component.serialization;

import java.io.IOException;

/**
 * Interface used to allow the proxy to support serialization. Currently used to work around
 * a limitation in the ProxyFactory
 * @author Stuart Douglas
 */
public interface WriteReplaceInterface {

    Object writeReplace() throws IOException;

}
