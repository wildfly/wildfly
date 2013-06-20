/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.rts.jaxrs;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.jboss.resteasy.core.AcceptHeaderByFileSuffixFilter;
import org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.providers.DataSourceProvider;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.DocumentProvider;
import org.jboss.resteasy.plugins.providers.FileProvider;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.plugins.providers.IIOImageProvider;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.JaxrsFormProvider;
import org.jboss.resteasy.plugins.providers.SerializableProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlRootElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlSeeAlsoProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlTypeProvider;
import org.jboss.resteasy.plugins.providers.jaxb.MapProvider;
import org.jboss.resteasy.plugins.providers.jaxb.XmlJAXBContextFinder;
import org.jboss.resteasy.plugins.providers.jaxb.json.JettisonElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.json.JettisonXmlRootElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.json.JettisonXmlSeeAlsoProvider;
import org.jboss.resteasy.plugins.providers.jaxb.json.JettisonXmlTypeProvider;
import org.jboss.resteasy.plugins.providers.jaxb.json.JsonCollectionProvider;
import org.jboss.resteasy.plugins.providers.jaxb.json.JsonJAXBContextFinder;
import org.jboss.resteasy.plugins.providers.jaxb.json.JsonMapProvider;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public abstract class AbstractRTSApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.addAll(getDefaultProviders());
        classes.addAll(getJaxbProviders());
        classes.addAll(getJettisonProviders());
        classes.addAll(getDefaultInterceptors());

        return classes;
    }

    private Set<Class<?>> getDefaultProviders() {
        Set<Class<?>> providers = new HashSet<>();

        // Message body writers / readers
        providers.add(DocumentProvider.class);
        providers.add(FormUrlEncodedProvider.class);
        providers.add(DefaultTextPlain.class);
        providers.add(SerializableProvider.class);
        providers.add(FileProvider.class);
        providers.add(InputStreamProvider.class);
        providers.add(JaxrsFormProvider.class);
        providers.add(StringTextStar.class);
        providers.add(IIOImageProvider.class);
        providers.add(DataSourceProvider.class);

        return providers;
    }

    private Set<Class<?>> getDefaultInterceptors() {
        Set<Class<?>> providers = new HashSet<>();

        providers.add(AcceptHeaderByFileSuffixFilter.class);
        providers.add(AcceptEncodingGZIPFilter.class);
        providers.add(GZIPEncodingInterceptor.class);
        providers.add(AcceptEncodingGZIPInterceptor.class);
        providers.add(GZIPDecodingInterceptor.class);

        return providers;
    }

    private Set<Class<?>> getJaxbProviders() {
        Set<Class<?>> providers = new HashSet<>();

        // Message body writers / readers
        providers.add(CollectionProvider.class);
        providers.add(JAXBXmlRootElementProvider.class);
        providers.add(JAXBXmlSeeAlsoProvider.class);
        providers.add(JAXBXmlTypeProvider.class);
        providers.add(JAXBElementProvider.class);
        providers.add(MapProvider.class);

        // Context resolvers
        providers.add(XmlJAXBContextFinder.class);

        return providers;
    }

    private Set<Class<?>> getJettisonProviders() {
        Set<Class<?>> providers = new HashSet<>();

        providers.add(JettisonElementProvider.class);
        providers.add(JettisonXmlTypeProvider.class);
        providers.add(JsonMapProvider.class);
        providers.add(JettisonXmlRootElementProvider.class);
        providers.add(JettisonXmlSeeAlsoProvider.class);
        providers.add(JsonCollectionProvider.class);
        providers.add(JsonJAXBContextFinder.class);

        return providers;
    }
}
