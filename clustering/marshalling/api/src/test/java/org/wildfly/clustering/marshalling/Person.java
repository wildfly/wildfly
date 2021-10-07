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

package org.wildfly.clustering.marshalling;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Paul Ferraro
 */
public class Person implements Comparable<Person>, java.io.Serializable {
    private static final long serialVersionUID = 7478927571966290859L;

    private volatile String name;
    private volatile Person parent;
    private final Set<Person> children = new TreeSet<>();

    public static Person create(String name) {
        Person person = new Person();
        person.setName(name);
        return person;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void addChild(Person child) {
        this.children.add(child);
        child.parent = this;
    }

    public Person getParent() {
        return this.parent;
    }

    public Iterable<Person> getChildren() {
        return this.children;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Person)) return false;
        return this.name.equals(((Person) object).name);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int compareTo(Person person) {
        return this.name.compareTo(person.name);
    }
}
