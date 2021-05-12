/*
 * Copyright 2021 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.opentracing.spi.sender;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.exceptions.SenderException;
import io.jaegertracing.spi.Sender;
import org.wildfly.extension.microprofile.opentracing.TracingExtensionLogger;

/**
 * Jaeger client Sende implementation to be able to 'swallow' exceptions when sending the spans to a Jaeger server.
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
public class WildFlySender implements Sender {

    private Sender delegate;

    WildFlySender(Sender sender) {
        TracingExtensionLogger.ROOT_LOGGER.debugf("Creating new WildFly Sender with %s", sender);
        this.delegate = sender;
    }

    @Override
    public int append(JaegerSpan span) throws SenderException {
        return delegate.append(span);
    }

    @Override
    public int flush() throws SenderException {
        try {
            return delegate.flush();
        } catch (SenderException ex) {
            TracingExtensionLogger.ROOT_LOGGER.debugf("Error while flushing %s spans", ex.getDroppedSpanCount(), ex);
            return 0;
        }
    }

    @Override
    public int close() throws SenderException {
        return delegate.close();
    }

}
