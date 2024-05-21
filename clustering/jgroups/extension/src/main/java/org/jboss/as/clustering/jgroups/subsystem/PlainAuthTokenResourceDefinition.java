/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public class PlainAuthTokenResourceDefinition extends AuthTokenResourceDefinition<BinaryAuthToken> {

    static final PathElement PATH = pathElement("plain");

    PlainAuthTokenResourceDefinition() {
        super(PATH, UnaryOperator.identity());
    }

    @Override
    public Map.Entry<Function<String, BinaryAuthToken>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return Map.entry(new Function<>() {
            @Override
            public BinaryAuthToken apply(String sharedSecret) {
                return new BinaryAuthToken(sharedSecret.getBytes(StandardCharsets.UTF_8));
            }
        }, Functions.discardingConsumer());
    }
}
