package org.jboss.as.messaging.jms.test;

import static java.beans.Introspector.getBeanInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class AttributesTestBase {

    /** compare everything using lower case */
    protected static void compare(String name1, SortedSet<String> set1,
            String name2, SortedSet<String> set2) {

        Set<String> onlyInSet1 = new TreeSet<>();
        for (String name : set1) {
            onlyInSet1.add(name.toLowerCase());
        }
        for (String name : set2) {
            onlyInSet1.remove(name.toLowerCase());
        }

        Set<String> onlyInSet2 = new TreeSet<>();
        for (String name : set2) {
            onlyInSet2.add(name.toLowerCase());
        }
        for (String name : set1) {
            onlyInSet2.remove(name.toLowerCase());
        }

        if (!onlyInSet1.isEmpty() || !onlyInSet2.isEmpty()) {
            fail(String.format("in %s only: %s\nin %s only: %s", name1, onlyInSet1, name2, onlyInSet2));
        }

        assertEquals(set2, set1);
    }

    protected SortedSet<String> findAllPropertyNames(Class<?> clazz) throws Exception {
        SortedSet<String> names = new TreeSet<String>();
        for (MethodDescriptor methodDescriptor : getBeanInfo(clazz).getMethodDescriptors()) {
            if (methodDescriptor.getName().startsWith("set")) {
                String attribute = methodDescriptor.getName().substring(3);
                names.add(attribute.toLowerCase());
            }
        }
        System.out.println("names = " + names);
        return names;
    }
}
