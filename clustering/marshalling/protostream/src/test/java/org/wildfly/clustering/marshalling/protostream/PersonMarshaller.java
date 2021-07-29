/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.Person;

/**
 * @author Paul Ferraro
 */
public class PersonMarshaller implements ProtoStreamMarshaller<Person> {

    private static final int NAME_INDEX = 1;
    private static final int PARENT_INDEX = 2;
    private static final int CHILD_INDEX = 3;

    @Override
    public Class<? extends Person> getJavaClass() {
        return Person.class;
    }

    @Override
    public Person readFrom(ProtoStreamReader reader) throws IOException {
        Person person = new Person();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case NAME_INDEX:
                    person.setName(reader.readString());
                    break;
                case PARENT_INDEX:
                    Person parent = reader.readAny(Person.class);
                    parent.addChild(person);
                    break;
                case CHILD_INDEX:
                    person.addChild(reader.readAny(Person.class));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return person;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Person person) throws IOException {
        String name = person.getName();
        if (name != null) {
            writer.writeString(NAME_INDEX, person.getName());
        }
        Person parent = person.getParent();
        if (parent != null) {
            writer.writeAny(PARENT_INDEX, parent);
        }
        for (Person child : person.getChildren()) {
            writer.writeAny(CHILD_INDEX, child);
        }
    }
}
