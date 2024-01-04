/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.client;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;

/**
 * {@link SerializationContextInitializer} service for this module
 * @author Paul Ferraro
 */

@MetaInfServices(SerializationContextInitializer.class)
public class TestEJBClientSerializationContextInitializer extends EJBClientSerializationContextInitializer {
}
