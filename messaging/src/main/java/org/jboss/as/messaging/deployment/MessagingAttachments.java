package org.jboss.as.messaging.deployment;

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
