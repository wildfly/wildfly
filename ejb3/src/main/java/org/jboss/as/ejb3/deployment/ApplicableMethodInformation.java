package org.jboss.as.ejb3.deployment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.ejb3.component.MethodIntf;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Metadata store for method level information that can be applied to a method via versious different deployment
 * descriptor styles
 *
 * @author Stuart Douglas
 */
public class ApplicableMethodInformation<T> {

    private final String componentName;

    /**
     * EJB 3.1 FR 13.3.7, the default transaction attribute is <i>REQUIRED</i>.
     */
    private T defaultAttribute;

    /**
     * applied to all view methods of a given type
     */
    private final Map<MethodIntf, T> perViewStyle1 = new HashMap<MethodIntf, T>();

    /**
     * Applied to all methods with a given name on a given view type
     */
    private final PopulatingMap<MethodIntf, Map<String, T>> perViewStyle2 = new PopulatingMap<MethodIntf, Map<String, T>>() {
        @Override
        Map<String, T> populate() {
            return new HashMap<String, T>();
        }
    };

    /**
     * Applied to an exact method on a view type
     */
    private final PopulatingMap<MethodIntf, PopulatingMap<String, Map<ArrayKey, T>>> perViewStyle3 = new PopulatingMap<MethodIntf, PopulatingMap<String, Map<ArrayKey, T>>>() {
        @Override
        PopulatingMap<String, Map<ArrayKey, T>> populate() {
            return new PopulatingMap<String, Map<ArrayKey, T>>() {
                @Override
                Map<ArrayKey, T> populate() {
                    return new HashMap<ArrayKey, T>();
                }
            };
        }
    };

    /**
     * Map of bean class to attribute
     */
    private final Map<String, T> style1 = new HashMap<String, T>();

    /**
     * map of bean method name to attribute
     */
    private final Map<String, T> style2 = new HashMap<String, T>();

    public ApplicableMethodInformation(final String componentName, final T defaultAttribute) {
        this.componentName = componentName;
        this.defaultAttribute = defaultAttribute;
    }

    /**
     * map of exact bean method to attribute
     */
    private final PopulatingMap<String, PopulatingMap<String, Map<ArrayKey, T>>> style3 = new PopulatingMap<String, PopulatingMap<String, Map<ArrayKey, T>>>() {
        @Override
        PopulatingMap<String, Map<ArrayKey, T>> populate() {
            return new PopulatingMap<String, Map<ArrayKey, T>>() {
                @Override
                Map<ArrayKey, T> populate() {
                    return new HashMap<ArrayKey, T>();
                }
            };
        }
    };

    public T getAttribute(MethodIntf methodIntf, String className, String methodName, String... methodParams) {
        assert methodIntf != null : "methodIntf is null";
        assert methodName != null : "methodName is null";
        assert methodParams != null : "methodParams is null";

        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        T attr = get(get(get(perViewStyle3, methodIntf), methodName), methodParamsKey);
        if (attr != null)
            return attr;
        attr = get(get(perViewStyle2, methodIntf), methodName);
        if (attr != null)
            return attr;
        attr = get(perViewStyle1, methodIntf);
        if (attr != null)
            return attr;
        attr = get(get(get(style3, className), methodName), methodParamsKey);
        if (attr != null)
            return attr;
        attr = get(style2, methodName);
        if (attr != null)
            return attr;
        attr = get(style1, className);
        if (attr != null)
            return attr;
        return defaultAttribute;
    }

    public T getViewAttribute(MethodIntf methodIntf, String methodName, String... methodParams) {
        assert methodIntf != null : "methodIntf is null";
        assert methodName != null : "methodName is null";
        assert methodParams != null : "methodParams is null";

        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        T attr = get(get(get(perViewStyle3, methodIntf), methodName), methodParamsKey);
        if (attr != null)
            return attr;
        attr = get(get(perViewStyle2, methodIntf), methodName);
        if (attr != null)
            return attr;
        attr = get(perViewStyle1, methodIntf);
        if (attr != null)
            return attr;
        return null;
    }

    /**
     * @param className The class name
     * @return The attribute that has been applied directly to the given class
     */
    public T getClassLevelAttribute(String className) {
        return style1.get(className);
    }


    /**
     * Style 1 (13.3.7.2.1 @1)
     *
     * @param methodIntf the method-intf the annotations apply to or null if EJB class itself
     * @param attribute
     */
    public void setAttribute(MethodIntf methodIntf, String className, T attribute) {
        if (methodIntf != null && className != null)
            throw MESSAGES.bothMethodIntAndClassNameSet(componentName);
        if (methodIntf == null) {
            style1.put(className, attribute);
        } else
            perViewStyle1.put(methodIntf, attribute);
    }

    public T getAttributeStyle1(MethodIntf methodIntf, String className) {
        if (methodIntf != null && className != null)
            throw MESSAGES.bothMethodIntAndClassNameSet(componentName);
        if (methodIntf == null) {
            return style1.get(className);
        } else {
            return perViewStyle1.get(methodIntf);
        }
    }

    /**
     * Style 2 (13.3.7.2.1 @2)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     * @param methodName
     */
    public void setAttribute(MethodIntf methodIntf, T transactionAttribute, String methodName) {
        if (methodIntf == null)
            style2.put(methodName, transactionAttribute);
        else
            perViewStyle2.pick(methodIntf).put(methodName, transactionAttribute);
    }

    public T getAttributeStyle2(MethodIntf methodIntf, String methodName) {
        if (methodIntf == null)
            return style2.get(methodName);
        else
            return perViewStyle2.pick(methodIntf).get(methodName);
    }

    /**
     * Style 3 (13.3.7.2.1 @3)
     *
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param transactionAttribute
     * @param methodName
     * @param methodParams
     */
    public void setAttribute(MethodIntf methodIntf, T transactionAttribute, final String className, String methodName, String... methodParams) {
        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        if (methodIntf == null)
            style3.pick(className).pick(methodName).put(methodParamsKey, transactionAttribute);
        else
            perViewStyle3.pick(methodIntf).pick(methodName).put(methodParamsKey, transactionAttribute);
    }

    public T getAttributeStyle3(MethodIntf methodIntf, final String className, String methodName, String... methodParams) {
        ArrayKey methodParamsKey = new ArrayKey((Object[]) methodParams);
        if (methodIntf == null)
            return style3.pick(className).pick(methodName).get(methodParamsKey);
        else
            return perViewStyle3.pick(methodIntf).pick(methodName).get(methodParamsKey);
    }

    public T getDefaultAttribute() {
        return defaultAttribute;
    }

    public void setDefaultAttribute(final T defaultAttribute) {
        this.defaultAttribute = defaultAttribute;
    }

    /**
     * Makes an array usable as a key.
     *
     * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
     */
    private static class ArrayKey {
        private final Object[] a;
        private final int hashCode;
        private transient String s;

        ArrayKey(Object... a) {
            this.a = a;
            this.hashCode = Arrays.hashCode(a);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ArrayKey))
                return false;
            return Arrays.equals(a, ((ArrayKey) obj).a);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            if (s == null)
                this.s = Arrays.toString(a);
            return s;
        }
    }

    /**
     * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
     */
    private abstract static class PopulatingMap<K, V> extends HashMap<K, V> {
        V pick(K key) {
            V value = get(key);
            if (value == null) {
                value = populate();
                put(key, value);
            }
            return value;
        }

        abstract V populate();
    }

    private static <K, V> V get(Map<K, V> map, K key) {
        if (map == null)
            return null;
        return map.get(key);
    }

}
