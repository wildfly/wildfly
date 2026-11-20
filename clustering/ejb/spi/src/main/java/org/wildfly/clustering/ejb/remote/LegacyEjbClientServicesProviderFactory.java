/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.remote;

/**
 * interface for obtaining EjbClientServicesProvider instances in the legacy case where no distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@Deprecated
public interface LegacyEjbClientServicesProviderFactory {
    EjbClientServicesProvider createEjbClientServicesProvider(String clusterName);
}
