package org.jboss.as.mail.extension;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;

/**
 * @author Tomaz Cerar
 * @created 22.12.11 18:31
 */
class MailServerWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
    static final MailServerWriteAttributeHandler INSTANCE = new MailServerWriteAttributeHandler();

    private MailServerWriteAttributeHandler() {
        super(MailServerDefinition.ATTRIBUTES);
    }


}
