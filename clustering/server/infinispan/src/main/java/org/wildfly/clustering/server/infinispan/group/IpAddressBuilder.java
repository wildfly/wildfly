/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.jgroups.stack.IpAddress;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamBuilder;

/**
 * @author Paul Ferraro
 */
public interface IpAddressBuilder extends ProtoStreamBuilder<IpAddress> {

    IpAddressBuilder setAddress(byte[] address) throws IOException;

    IpAddressBuilder setPort(int port);
}
