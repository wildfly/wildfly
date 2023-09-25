/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.IOException;

import org.junit.Assert;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractCircularReferenceTestCase {

    private final MarshallingTesterFactory factory;

    public AbstractCircularReferenceTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @org.junit.Test
    public void test() throws IOException {
        Person parent = Person.create("parent");
        Person self = Person.create("self");
        parent.addChild(self);
        parent.addChild(Person.create("sibling"));
        self.addChild(Person.create("son"));
        self.addChild(Person.create("daughter"));

        Tester<Person> tester = this.factory.createTester();
        tester.test(self, (expected, actual) -> {
            Assert.assertEquals(expected, actual);
            Assert.assertEquals(expected.getParent(), actual.getParent());
            Assert.assertEquals(expected.getChildren(), actual.getChildren());
            // Validate referential integrity
            for (Person child : actual.getParent().getChildren()) {
                Assert.assertSame(actual.getParent(), child.getParent());
            }
            for (Person child : actual.getChildren()) {
                Assert.assertSame(actual, child.getParent());
            }
        });
    }
}
