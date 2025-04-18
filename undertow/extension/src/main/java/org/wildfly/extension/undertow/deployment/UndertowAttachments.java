/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.deployment;

import java.io.File;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.wildfly.extension.undertow.ServletContainerService;

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

    @Deprecated
    public static final AttachmentKey<SharedSessionManagerConfig> SHARED_SESSION_MANAGER_CONFIG = SharedSessionManagerConfig.ATTACHMENT_KEY;

    public static final AttachmentKey<WebSocketDeploymentInfo> WEB_SOCKET_DEPLOYMENT_INFO = AttachmentKey.create(WebSocketDeploymentInfo.class);

    public static final AttachmentKey<AttachmentList<File>> EXTERNAL_RESOURCES = AttachmentKey.createList(File.class);

    public static final AttachmentKey<ServletContainerService> SERVLET_CONTAINER_SERVICE = AttachmentKey.create(ServletContainerService.class);

    public static final AttachmentKey<AttachmentList<Predicate>> ALLOW_REQUEST_WHEN_SUSPENDED = AttachmentKey.createList(Predicate.class);

    public static final AttachmentKey<String> DEFAULT_SECURITY_DOMAIN = AttachmentKey.create(String.class);

    public static final AttachmentKey<String> RESOLVED_SECURITY_DOMAIN = AttachmentKey.create(String.class);

    private UndertowAttachments() {
    }

}
