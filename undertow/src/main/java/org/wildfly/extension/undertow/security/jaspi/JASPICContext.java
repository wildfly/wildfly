package org.wildfly.extension.undertow.security.jaspi;

import io.undertow.util.AttachmentKey;
import org.jboss.security.auth.callback.JASPICallbackHandler;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;

import javax.security.auth.message.MessageInfo;

/**
 * @author Stuart Douglas
 */
public class JASPICContext {

    public static final AttachmentKey<JASPICContext> ATTACHMENT_KEY = AttachmentKey.create(JASPICContext.class);

    private final MessageInfo messageInfo;
    private final JASPIServerAuthenticationManager sam;
    private final JASPICallbackHandler cbh;

    public JASPICContext(MessageInfo messageInfo, JASPIServerAuthenticationManager sam, JASPICallbackHandler cbh) {
        this.messageInfo = messageInfo;
        this.sam = sam;
        this.cbh = cbh;
    }

    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    public JASPIServerAuthenticationManager getSam() {
        return sam;
    }

    public JASPICallbackHandler getCbh() {
        return cbh;
    }
}
