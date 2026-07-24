/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.arjuna.orbportability.common.OrbPortabilityEnvironmentBean;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Sets up the {@link OrbPortabilityEnvironmentBean}
 *
 * @author <a href="mailto:jfinelli@redhat.com">Manuel Finelli</a>
 */
public final class OrbPortabilityEnvironmentService implements Service {

    private final Consumer<OrbPortabilityEnvironmentBean> orbPortabilityEnvironmentBeanConsumer;
    private final Supplier<ServerEnvironment> serverEnvironmentSupplier;

    public OrbPortabilityEnvironmentService(final Consumer<OrbPortabilityEnvironmentBean> orbPortabilityEnvironmentBeanConsumer,
                                            final Supplier<ServerEnvironment> serverEnvironmentSupplier) {
        this.orbPortabilityEnvironmentBeanConsumer = orbPortabilityEnvironmentBeanConsumer;
        this.serverEnvironmentSupplier = serverEnvironmentSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final OrbPortabilityEnvironmentBean orbPortabilityEnvironmentBean =
                BeanPopulator.getDefaultInstance(OrbPortabilityEnvironmentBean.class);
        orbPortabilityEnvironmentBean.setInitialReferencesRoot(
                serverEnvironmentSupplier.get().getServerDataDir().getAbsolutePath());
        orbPortabilityEnvironmentBeanConsumer.accept(orbPortabilityEnvironmentBean);
    }

    @Override
    public void stop(final StopContext context) {
        final OrbPortabilityEnvironmentBean orbPortabilityEnvironmentBean =
                BeanPopulator.getDefaultInstance(OrbPortabilityEnvironmentBean.class);
        orbPortabilityEnvironmentBean.setInitialReferencesRoot(null);
        orbPortabilityEnvironmentBeanConsumer.accept(null);
    }
}
