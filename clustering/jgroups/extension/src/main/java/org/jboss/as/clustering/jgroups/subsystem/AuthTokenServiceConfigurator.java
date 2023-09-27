/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.AuthTokenResourceDefinition.Capability.AUTH_TOKEN;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CredentialSourceDependency;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.auth.AuthToken;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * @author Paul Ferraro
 */
public abstract class AuthTokenServiceConfigurator<T extends AuthToken> extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Function<String, T>, Dependency {

    private static final Function<CredentialSource, String> CREDENTIAL_SOURCE_MAPPER = new Function<>() {
        @Override
        public String apply(CredentialSource sharedSecretSource) {
            try {
                PasswordCredential credential = sharedSecretSource.getCredential(PasswordCredential.class);
                ClearPassword password = credential.getPassword(ClearPassword.class);
                return String.valueOf(password.getPassword());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    };

    private volatile SupplierDependency<CredentialSource> sharedSecretSource;

    public AuthTokenServiceConfigurator(PathAddress address) {
        super(AUTH_TOKEN, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.sharedSecretSource = new CredentialSourceDependency(context, AuthTokenResourceDefinition.Attribute.SHARED_SECRET, model);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<T> token = this.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(token, CREDENTIAL_SOURCE_MAPPER.andThen(this), this.sharedSecretSource);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public <V> ServiceBuilder<V> register(ServiceBuilder<V> builder) {
        return this.sharedSecretSource.register(builder);
    }
}
