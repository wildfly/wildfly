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

package org.jboss.as.messaging;

import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.hornetq.api.core.TransportConfiguration;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class InVMAcceptorSpec extends AbstractTransportElement<InVMAcceptorSpec> {

    private static final long serialVersionUID = -1268193638124263821L;
    private static final String FACTORY_CLASS = "org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory";

    private int serverId;

    protected InVMAcceptorSpec(String name) {
        super(Element.IN_VM_ACCEPTOR, name);
    }

    /** {@inheritDoc} */
    public String getFactoryClassName() {
        return FACTORY_CLASS;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    /** {@inheritDoc} */
    protected void processHQConfig(TransportConfiguration transport) {
        super.processHQConfig(transport);
        final Map<String, Object> map = transport.getParams();
        if(map != null) {
            map.put("server-id", "" + serverId);
        }
    }

    /** {@inheritDoc} */
    protected Class<InVMAcceptorSpec> getElementClass() {
        return InVMAcceptorSpec.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.SERVER_ID.getLocalName(), "" + serverId);
        super.writeContent(streamWriter);
    }

}
