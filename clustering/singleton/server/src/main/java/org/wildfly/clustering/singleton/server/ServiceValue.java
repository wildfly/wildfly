/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.value.Value;
import org.wildfly.clustering.server.service.Service;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface ServiceValue<T> extends Service, Value<T> {

}
