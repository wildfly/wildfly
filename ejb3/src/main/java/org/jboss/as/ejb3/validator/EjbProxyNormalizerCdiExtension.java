/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.validator;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class EjbProxyNormalizerCdiExtension implements Extension {

    public void addEjbProxyNormalizer(@Observes AfterBeanDiscovery afterBeanDiscovery){
        afterBeanDiscovery.addBean(new EjbProxyBeanMetaDataClassNormalizer());
    }
}
