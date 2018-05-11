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

import static org.jboss.as.clustering.jgroups.subsystem.DigestAuthTokenResourceDefinition.Attribute.ALGORITHM;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jgroups.auth.MD5Token;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Builds an AUTH token, functionally equivalent to {@link MD5Token}, but can use any digest algorithm supported by the default security provider.
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
