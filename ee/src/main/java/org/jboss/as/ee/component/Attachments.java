/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionResolver;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

/**
 * @author John Bailey
 */
public class Attachments {
    public static final AttachmentKey<AttachmentList<ComponentConfiguration>> COMPONENT_CONFIGS = AttachmentKey.createList(ComponentConfiguration.class);

    public static final AttachmentKey<ComponentFactory> COMPONENT_FACTORY = AttachmentKey.create(ComponentFactory.class);

    public static final AttachmentKey<ResourceInjectionResolver> RESOURCE_INJECTION_RESOLVER = AttachmentKey.create(ResourceInjectionResolver.class);

    @SuppressWarnings({"RawUseOfParameterizedType"})
    public static final AttachmentKey<Class> COMPONENT_CLASS = AttachmentKey.create(Class.class);

    public static final AttachmentKey<AttachmentList<ComponentLifecycle>> POST_CONSTRUCTS = AttachmentKey.createList(ComponentLifecycle.class);

    public static final AttachmentKey<AttachmentList<ComponentLifecycle>> PRE_DESTROYS = AttachmentKey.createList(ComponentLifecycle.class);

    public static final AttachmentKey<AttachmentList<ResourceInjectionResolver.ResolverResult>> RESOLVED_RESOURCES = AttachmentKey.createList(ResourceInjectionResolver.ResolverResult.class);

    public static final AttachmentKey<ComponentInterceptorFactories> COMPONENT_INTERCEPTOR_FACTORIES = AttachmentKey.create(ComponentInterceptorFactories.class);

    public static final AttachmentKey<AttachmentList<ResourceInjection>> RESOURCE_INJECTIONS = AttachmentKey.createList(ResourceInjection.class);

}
