/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.AuthTokenResourceDefinition.Capability.AUTH_TOKEN;

import java.io.IOException;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CredentialSourceDependency;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.auth.AuthToken;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.MappedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * @author Paul Ferraro
 */
public abstract class AuthTokenBuilder<T extends AuthToken> extends CapabilityServiceNameProvider implements ResourceServiceBuilder<T>, Function<String, T> {
    private volatile ValueDependency<CredentialSource> sharedSecretSource;

    public AuthTokenBuilder(PathAddress address) {
        super(AUTH_TOKEN, address);
    }

    @Override
    public Builder<T> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.sharedSecretSource = new CredentialSourceDependency(context, AuthTokenResourceDefinition.Attribute.SHARED_SECRET, model);
        return this;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        Function<CredentialSource, String> sharedSecret = sharedSecretSource -> {
            try {
                PasswordCredential credential = sharedSecretSource.getCredential(PasswordCredential.class);
                ClearPassword password = credential.getPassword(ClearPassword.class);
                return String.valueOf(password.getPassword());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        };
        return this.sharedSecretSource.register(target.addService(this.getServiceName(), new MappedValueService<>(sharedSecret.andThen(this), this.sharedSecretSource)));
    }
}
