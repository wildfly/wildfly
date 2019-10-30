package org.wildfly.extension.messaging.activemq.jms;

import static java.beans.Introspector.getBeanInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.beans.PropertyDescriptor;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class AttributesTestBase {

    protected static void compare(String name1, SortedSet<String> set1,
            String name2, SortedSet<String> set2) {
        Set<String> onlyInSet1 = new TreeSet<String>(set1);

        onlyInSet1.removeAll(set2);

        Set<String> onlyInSet2 = new TreeSet<String>(set2);
        onlyInSet2.removeAll(set1);

        if (!onlyInSet1.isEmpty() || !onlyInSet2.isEmpty()) {
            fail(String.format("in %s only: %s\nin %s only: %s", name1, onlyInSet1, name2, onlyInSet2));
        }

        assertEquals(set2, set1);
    }

    protected SortedSet<String> findAllPropertyNames(Class<?> clazz) throws Exception {
        SortedSet<String> names = new TreeSet<String>();
        for (PropertyDescriptor propDesc : getBeanInfo(clazz).getPropertyDescriptors()) {
            if (propDesc == null
                    || propDesc.getWriteMethod() == null) {
                continue;
            }
            names.add(propDesc.getDisplayName());
        }
        return names;
    }
}
