/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.deployment;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.faces.view.ViewScoped;

/**
 * Workaround for WFLY-3044 / JAVASERVERFACES-3191 making {@link ViewScoped} annotation passivating. Proper fix
 * requires a spec maintenance release.
 *
 * @author <a href="http://www.radoslavhusar.com/">Radoslav Husar</a>
 * @version Apr 10, 2014
 * @since 8.0.1
 */
public class JSFPassivatingViewScopedCdiExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        event.addScope(ViewScoped.class, true, true);
    }
}
