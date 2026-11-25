/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonState;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceController;
import org.wildfly.service.ServiceDependency;

/**
 * A service builder that installs a local singleton service.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceBuilder<T> extends DelegatingServiceBuilder<T> implements SingletonServiceBuilder<T> {

    private final Supplier<SingletonState> localState;
    private volatile SingletonElectionListener listener = null;
    private final Consumer<Singleton> singleton;
    private final SingletonReference reference = new SingletonReference();

    public LocalSingletonServiceBuilder(ServiceBuilder<T> builder, ServiceDependency<GroupMember> member, Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory) {
        super(builder);
        member.accept(builder);
        this.localState = member.map(LocalSingletonState::new);
        this.singleton = singletonFactory.apply(builder).andThen(this.reference);
    }

    @Override
    public SingletonServiceBuilder<T> addListener(LifecycleListener listener) {
        this.getDelegate().addListener(listener);
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> setInstance(Service service) {
        Singleton localSingleton = new SimpleSingleton(this.localState);
        this.getDelegate().setInstance(new Service() {
            @Override
            public void start(StartContext context) throws StartException {
                service.start(context);
                LocalSingletonServiceBuilder.this.singleton.accept(localSingleton);
            }

            @Override
            public void stop(StopContext context) {
                LocalSingletonServiceBuilder.this.singleton.accept(null);
                service.stop(context);
            }
        });
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> setInitialMode(ServiceController.Mode mode) {
        this.getDelegate().setInitialMode(mode);
        return this;
    }

    @Override
    public SingletonServiceController<T> install() {
        Supplier<SingletonState> state = this.localState;
        SingletonElectionListener listener = this.listener;
        if (listener != null) {
            // Trigger election listener when service starts
            this.getDelegate().addListener(new LifecycleListener() {
                @Override
                public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                    if (event == LifecycleEvent.UP) {
                        GroupMember elected = state.get().getPrimaryProvider().get();
                        listener.elected(List.of(elected), elected);
                    }
                }
            });
        }
        return new DistributedSingletonServiceController<>(this.getDelegate().install(), this.reference);
    }

    @Override
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy) {
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener) {
        this.listener = listener;
        return this;
    }
}
