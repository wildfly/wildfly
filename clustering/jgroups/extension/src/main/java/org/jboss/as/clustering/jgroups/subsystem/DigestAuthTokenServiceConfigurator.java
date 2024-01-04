/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.DigestAuthTokenResourceDefinition.Attribute.ALGORITHM;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Builds an AUTH token, functionally equivalent to {@link org.jgroups.auth.MD5Token}, but can use any digest algorithm supported by the default security provider.
 * @author Paul Ferraro
 */
public class DigestAuthTokenServiceConfigurator extends AuthTokenServiceConfigurator<BinaryAuthToken> {

    private volatile String algorithm;

    public DigestAuthTokenServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.algorithm = ALGORITHM.resolveModelAttribute(context, model).asString();
        return super.configure(context, model);
    }

    @Override
    public BinaryAuthToken apply(String sharedSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance(this.algorithm);
            return new BinaryAuthToken(digest.digest(sharedSecret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
