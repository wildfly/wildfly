/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a digest authentication token for use by the AUTH protocol.
 * @author Paul Ferraro
 */
public class DigestAuthTokenResourceDefinitionRegistrar extends AuthTokenResourceDefinitionRegistrar<BinaryAuthToken> {

    DigestAuthTokenResourceDefinitionRegistrar() {
        super(DigestAuthTokenResourceDescription.INSTANCE);
    }

    @Override
    public ServiceDependency<Function<byte[], BinaryAuthToken>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String algorithm = DigestAuthTokenResourceDescription.Attribute.ALGORITHM.resolveModelAttribute(context, model).asString();
        return ServiceDependency.of(new Function<>() {
            @Override
            public BinaryAuthToken apply(byte[] secret) {
                try {
                    MessageDigest digest = MessageDigest.getInstance(algorithm);
                    return new BinaryAuthToken(digest.digest(secret));
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }
}
