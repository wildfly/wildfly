/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.SetupAction;

import java.util.List;
import java.util.Map;

/**
 * @author emartins
 */
public class ConcurrencyAttachments {

    public static final AttachmentKey<AttachmentList<ContextHandleFactory>> ADDITIONAL_FACTORIES = AttachmentKey.createList(ContextHandleFactory.class);

    public static final AttachmentKey<Map<ComponentConfiguration, List<ContextHandleFactory>>> ADDITIONAL_COMPONENT_FACTORIES = AttachmentKey.create(Map.class);

    public static final AttachmentKey<SetupAction> CONCURRENT_CONTEXT_SETUP_ACTION = AttachmentKey.create(SetupAction.class);
}
