/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        reader.getContext().record(person);
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
        writer.getContext().record(person);
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
