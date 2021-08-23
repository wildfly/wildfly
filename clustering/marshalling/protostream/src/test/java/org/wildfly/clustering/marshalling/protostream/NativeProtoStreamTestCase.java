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
import java.util.Objects;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;

/**
 * Test marshalling of class that use native ProtoStream annotations.
 * @author Paul Ferraro
 */
public class NativeProtoStreamTestCase {

    @Test
    public void test() throws IOException {
        MarshallingTesterFactory factory = ProtoStreamTesterFactory.INSTANCE;
        factory.createTester(Sex.class).test();

        Employee head = new Employee(1, new Name("Allegra", "Coleman"), Sex.FEMALE, null);
        Employee manager = new Employee(2, new Name("John", "Barron"), Sex.MALE, head);
        Employee employee = new Employee(3, new Name("Alan", "Smithee"), Sex.MALE, manager);

        factory.<Employee>createTester().test(employee, NativeProtoStreamTestCase::equals);
    }

    static void equals(Employee expected, Employee actual) {
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertSame(expected.getSex(), actual.getSex());
        Assert.assertEquals(expected.isHead(), actual.isHead());
        if (!expected.isHead()) {
            equals(expected.getManager(), actual.getManager());
        }
    }

    static enum Sex {
        @ProtoEnumValue(0) MALE,
        @ProtoEnumValue(1) FEMALE,
        ;
    }

    static class Employee {

        private final Integer id;
        private final Name name;
        private final Sex sex;
        private final Employee manager;

        @ProtoFactory
        Employee(Integer id, Name name, Sex sex, Employee manager) {
            this.id = id;
            this.name = name;
            this.sex = sex;
            this.manager = manager;
        }

        @ProtoField(1)
        Integer getId() {
            return this.id;
        }

        @ProtoField(2)
        Name getName() {
            return this.name;
        }

        @ProtoField(3)
        Sex getSex() {
            return this.sex;
        }

        @ProtoField(4)
        Employee getManager() {
            return this.manager;
        }

        boolean isHead() {
            return this.manager == null;
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Employee)) return false;
            Employee employee = (Employee) object;
            return Objects.equals(this.id, employee.id);
        }
    }

    static class Name {
        private final String first;
        private final String last;

        Name(String first, String last) {
            this.first = first;
            this.last = last;
        }

        String getFirst() {
            return this.first;
        }

        String getLast() {
            return this.last;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Name)) return false;
            Name name = (Name) object;
            return Objects.equals(this.first, name.first) && Objects.equals(this.last, name.last);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.last, this.first);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (this.last != null) {
                builder.append(this.last);
                if (this.first != null) {
                    builder.append(", ").append(this.first);
                }
            }
            return builder.toString();
        }
    }

    @ProtoAdapter(Name.class)
    static class NameFactory {
        @ProtoFactory
        static Name create(String first, String last) {
            return new Name(first, last);
        }

        @ProtoField(1)
        static String getFirst(Name name) {
            return name.getFirst();
        }

        @ProtoField(2)
        static String getLast(Name name) {
            return name.getLast();
        }
    }

    @AutoProtoSchemaBuilder(includeClasses = { Sex.class, NameFactory.class, Employee.class }, service = false)
    static interface EmployeeInitializer extends SerializationContextInitializer {
    }
}
