/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.faces.mojarra;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.faces.mojarra.context.flash.ContextFlashMarshallerProvider;
import org.wildfly.clustering.faces.mojarra.facelets.el.FaceletsELMarshallerProvider;
import org.wildfly.clustering.faces.mojarra.util.UtilMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;

/**
 * @author Paul Ferraro
 */
public enum MojarraSerializationContextInitializerProvider implements SerializationContextInitializerProvider {

    CONTEXT_FLASH(new ProviderSerializationContextInitializer<>("com.sun.faces.context.flash.proto", ContextFlashMarshallerProvider.class)),
    FACELETS_EL(new ProviderSerializationContextInitializer<>("com.sun.faces.facelets.el.proto", FaceletsELMarshallerProvider.class)),
    UTIL(new ProviderSerializationContextInitializer<>("com.sun.faces.util.proto", UtilMarshallerProvider.class)),
    ;
    private final SerializationContextInitializer initializer;

    MojarraSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
