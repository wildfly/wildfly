/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.marshall;

import org.infinispan.commons.dataconversion.ByteArrayWrapper;
import org.infinispan.commons.dataconversion.DefaultTranscoder;
import org.infinispan.commons.dataconversion.IdentityWrapper;
import org.infinispan.commons.dataconversion.TranscoderMarshallerAdapter;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.encoding.ProtostreamTranscoder;
import org.infinispan.factories.AbstractComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.impl.ComponentRef;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;

/**
 * Overrides Infinispan's default encoder registry.
 * @author Paul Ferraro
 */
@DefaultFactoryFor(classes = { org.infinispan.marshall.core.EncoderRegistry.class })
public class EncoderRegistryFactory extends AbstractComponentFactory implements AutoInstantiableFactory {

    // Must not start the global marshaller or it will be too late for modules to register their externalizers
    @Inject @ComponentName(KnownComponentNames.INTERNAL_MARSHALLER)
    ComponentRef<Marshaller> internalMarshaller;

    @Inject @ComponentName(KnownComponentNames.USER_MARSHALLER)
    Marshaller marshaller;

    @Inject EmbeddedCacheManager manager;
    @Inject SerializationContextRegistry ctxRegistry;

    @SuppressWarnings("deprecation")
    @Override
    public Object construct(String componentName) {
        ClassLoader classLoader = this.globalConfiguration.classLoader();
        EncoderRegistry encoderRegistry = new DefaultEncoderRegistry();

        encoderRegistry.registerEncoder(org.infinispan.commons.dataconversion.IdentityEncoder.INSTANCE);
        encoderRegistry.registerEncoder(org.infinispan.commons.dataconversion.UTF8Encoder.INSTANCE);
        encoderRegistry.registerEncoder(new org.infinispan.commons.dataconversion.GlobalMarshallerEncoder(this.internalMarshaller.wired()));

        // Default and binary transcoder use the user marshaller to convert data to/from a byte array
        encoderRegistry.registerTranscoder(new DefaultTranscoder(this.marshaller));
        // Handle application/unknown
        encoderRegistry.registerTranscoder(new org.infinispan.commons.dataconversion.BinaryTranscoder(this.marshaller));
        // Core transcoders are always available
        encoderRegistry.registerTranscoder(new ProtostreamTranscoder(this.ctxRegistry, classLoader));
        // Wraps the GlobalMarshaller so that it can be used as a transcoder
        // Keeps application/x-infinispan-marshalling available for backwards compatibility
        encoderRegistry.registerTranscoder(new TranscoderMarshallerAdapter(this.internalMarshaller.wired()));
        // Make the user marshaller's media type available as well
        encoderRegistry.registerTranscoder(new TranscoderMarshallerAdapter(this.marshaller));

        encoderRegistry.registerWrapper(ByteArrayWrapper.INSTANCE);
        encoderRegistry.registerWrapper(IdentityWrapper.INSTANCE);

        return encoderRegistry;
    }
}
