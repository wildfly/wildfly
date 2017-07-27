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
package org.wildfly.extension.undertow.deployment;

import java.io.File;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;

import io.undertow.predicate.Predicate;
import io.undertow.server.HandlerWrapper;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

/**
 * Class defining {@link AttachmentKey}s for Undertow-specific attachments.
 *
 * @author Radoslav Husar
 * @version Oct 2013
 * @since 8.0
 */
public final class UndertowAttachments {

    public static final AttachmentKey<AttachmentList<HandlerWrapper>> UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS = AttachmentKey.createList(HandlerWrapper.class);

    public static final AttachmentKey<AttachmentList<HandlerWrapper>> UNDERTOW_INNER_HANDLER_CHAIN_WRAPPERS = AttachmentKey.createList(HandlerWrapper.class);

    public static final AttachmentKey<AttachmentList<HandlerWrapper>> UNDERTOW_OUTER_HANDLER_CHAIN_WRAPPERS = AttachmentKey.createList(HandlerWrapper.class);

    public static final AttachmentKey<AttachmentList<ThreadSetupHandler>> UNDERTOW_THREAD_SETUP_ACTIONS = AttachmentKey.createList(ThreadSetupHandler.class);

    public static final AttachmentKey<AttachmentList<ServletExtension>> UNDERTOW_SERVLET_EXTENSIONS = AttachmentKey.createList(ServletExtension.class);

    public static final AttachmentKey<SharedSessionManagerConfig> SHARED_SESSION_MANAGER_CONFIG = AttachmentKey.create(SharedSessionManagerConfig.class);

    public static final AttachmentKey<WebSocketDeploymentInfo> WEB_SOCKET_DEPLOYMENT_INFO = AttachmentKey.create(WebSocketDeploymentInfo.class);

    public static final AttachmentKey<AttachmentList<File>> EXTERNAL_RESOURCES = AttachmentKey.createList(File.class);

    public static final AttachmentKey<ServletContainerService> SERVLET_CONTAINER_SERVICE = AttachmentKey.create(ServletContainerService.class);

    public static final AttachmentKey<AttachmentList<Predicate>> ALLOW_REQUEST_WHEN_SUSPENDED = AttachmentKey.createList(Predicate.class);

    public static final AttachmentKey<String> DEFAULT_SECURITY_DOMAIN = AttachmentKey.create(String.class);

    private UndertowAttachments() {
    }

}
