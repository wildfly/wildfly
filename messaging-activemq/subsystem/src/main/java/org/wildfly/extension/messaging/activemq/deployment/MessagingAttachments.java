/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

/**
 *
 *
 * @author Stuart Douglas
 */
public class MessagingAttachments {

    static final AttachmentKey<AttachmentList<ParseResult>> PARSE_RESULT = AttachmentKey.createList(ParseResult.class);


}
