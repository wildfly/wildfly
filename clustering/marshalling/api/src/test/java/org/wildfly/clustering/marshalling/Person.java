/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
