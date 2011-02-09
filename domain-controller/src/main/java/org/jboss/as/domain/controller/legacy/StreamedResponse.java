/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.legacy;

/**
 * Wrapper for a value that can be one in a stream of related values sent
 * in response to {@link DomainClientProtocol} request.
 *
 * @author Brian Stansberry
 */
public class StreamedResponse {

    private final Object value;
    private final byte protocolValue;
    private final boolean isLastInStream;


    public StreamedResponse(final byte protocolValue, final Object value) {
        this(protocolValue, value, false);
    }

    public StreamedResponse(final byte protocolValue, final Object value, final boolean isLastInStream) {
        this.value = value;
        this.protocolValue = protocolValue;
        this.isLastInStream = isLastInStream;
    }

    public boolean isLastInStream() {
        return isLastInStream;
    }
    public byte getProtocolValue() {
        return protocolValue;
    }
    public Object getValue() {
        return value;
    }
}
