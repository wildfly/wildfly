/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.property;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A property editor for {@link java.net.InetAddress}.
 *
 *
 * @author <a href="mailto:Adrian.Brock@HappeningTimes.com">Adrian Brock</a>
 */
public class InetAddressEditor extends TextPropertyEditorSupport {

    /**
     * Returns a InetAddress for the input object converted to a string.
     *
     * @return an InetAddress
     *
     * @throws NestedRuntimeException An UnknownHostException occured.
     */
    public Object getValue() {
        try {
            String text = getAsText();
            if (text == null) {
                return null;
            }
            if (text.startsWith("/")) {
                // seems like localhost sometimes will look like:
                // /127.0.0.1 and the getByNames barfs on the slash - JGH
                text = text.substring(1);
            }
            return InetAddress.getByName(StringPropertyReplacer.replaceProperties(text));
        } catch (UnknownHostException e) {
            throw new NestedRuntimeException(e);
        }
    }
}
