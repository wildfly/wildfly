/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;
import java.util.function.Supplier;

import org.jgroups.stack.IpAddress;

/**
 * @author Paul Ferraro
 */
public interface IpAddressBuilder extends Supplier<IpAddress> {

    IpAddressBuilder setAddress(byte[] address) throws IOException;

    IpAddressBuilder setPort(int port);
}
