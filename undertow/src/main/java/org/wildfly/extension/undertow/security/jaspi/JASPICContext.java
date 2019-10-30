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
