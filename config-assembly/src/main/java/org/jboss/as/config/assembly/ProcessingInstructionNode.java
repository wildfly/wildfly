/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ProcessingInstructionNode extends Node {
    private final String name;
    private final Map<String, String> data;
    private List<Node> delegates = new ArrayList<Node>();

    public ProcessingInstructionNode(final String name, final Map<String, String> data) {
        this.name = name;
        this.data = data;
    }

    void addDelegate(Node delegate) {
        if (delegate != null) {
            delegates.add(delegate);
        }
    }

    String getDataValue(String name, String defaultValue) {
        if (data != null) {
            String s = data.get(name);
            if (s != null) {
                return s;
            }
        }
        return defaultValue;
    }

    @Override
    void marshall(XMLStreamWriter writer) throws XMLStreamException {
        for (Node delegate : delegates) {
            delegate.marshall(writer);
        }
    }

    boolean hasContent() {
        for (Node delegate : delegates) {
            if (delegate.hasContent()) {
                return true;
            }
        }
        return false;
    }

}
