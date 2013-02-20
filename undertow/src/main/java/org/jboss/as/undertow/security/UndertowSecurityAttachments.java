package org.jboss.as.undertow.security;

import io.undertow.util.AttachmentKey;
import org.jboss.security.SecurityContext;

/**
 * @author Stuart Douglas
 */
public class UndertowSecurityAttachments {

    public static final AttachmentKey<SecurityContext> SECURITY_CONTEXT_ATTACHMENT = AttachmentKey.create(SecurityContext.class);

}
