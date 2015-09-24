package org.wildfly.extension.undertow.security.jaspi;

import io.undertow.security.idm.Account;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;
import org.jboss.security.auth.callback.JASPICallbackHandler;
import org.jboss.security.auth.message.GenericMessageInfo;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;

/**
 * @author Stuart Douglas
 */
class JASPICAttachment {

    public static final AttachmentKey<JASPICAttachment> ATTACHMENT_KEY = AttachmentKey.create(JASPICAttachment.class);

    private Boolean valid;
    private final ServletRequestContext requestContext;
    private final JASPIServerAuthenticationManager sam;
    private final GenericMessageInfo messageInfo;
    private final String applicationIdentifier;
    private final JASPICallbackHandler cbh;
    private final Account cachedAccount;

    JASPICAttachment(boolean isValid, ServletRequestContext requestContext, JASPIServerAuthenticationManager sam, GenericMessageInfo messageInfo, String applicationIdentifier, JASPICallbackHandler cbh, Account cachedAccount) {
        this.valid = isValid;
        this.requestContext = requestContext;
        this.sam = sam;
        this.messageInfo = messageInfo;
        this.applicationIdentifier = applicationIdentifier;
        this.cbh = cbh;
        this.cachedAccount = cachedAccount;
    }

    public boolean getValid() {
        return valid;
    }

    public ServletRequestContext getRequestContext() {
        return requestContext;
    }

    public JASPIServerAuthenticationManager getSam() {
        return sam;
    }

    public GenericMessageInfo getMessageInfo() {
        return messageInfo;
    }

    public String getApplicationIdentifier() {
        return applicationIdentifier;
    }

    public JASPICallbackHandler getCbh() {
        return cbh;
    }

    public Account getCachedAccount() {
        return cachedAccount;
    }

    public void setValid(Boolean isValid) {
        this.valid = isValid;
    }
}
