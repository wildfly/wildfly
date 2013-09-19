package org.jboss.as.logging.logmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logmanager.config.PropertyConfigurable;
import org.jboss.logmanager.config.ValueExpression;

/**
 * An interfaced used to determine if properties should be resorted and if so sort them.
 * <p/>
 * This is useful when certain properties need to be configured before others. For example a {@code FileHandler} may
 * allow for an {@code append} property that needs to be set before the file is set.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface PropertySorter {

    /**
     * Checks the properties and returns {@code true} if the properties should be sorted by invoking {@link
     * #sort(org.jboss.logmanager.config.PropertyConfigurable)}, otherwise {@code false}.
     *
     * @param configurable the configurable to check
     *
     * @return {@code true} if the properties should be sorted, otherwise {@code false}
     */
    boolean isReorderRequired(PropertyConfigurable configurable);

    /**
     * Sorts the properties.
     *
     * @param configurable the configurable to sort the properties on
     */
    void sort(PropertyConfigurable configurable);

    /**
     * A default no-op sorter that always returns {@code false} on the {@link #isReorderRequired(org.jboss.logmanager.config.PropertyConfigurable)}
     * method. The {@link #sort(org.jboss.logmanager.config.PropertyConfigurable)} does nothing.
     */
    PropertySorter NO_OP = new PropertySorter() {
        @Override
        public boolean isReorderRequired(final PropertyConfigurable configurable) {
            return false;
        }

        @Override
        public void sort(final PropertyConfigurable configurable) {
        }
    };

    /**
     * A default configurator that uses a {@link Comparator comparator} to determine whether the properties should be
     * sorted and the sort order.
     * <p/>
     * <i>Note: In most cases the {@link Comparator comparator} will impose orderings consistent with equals which is
     * acceptable usage for this sorter</i>
     */
    public static class DefaultPropertySorter implements PropertySorter {

        private final Comparator<String> comparator;

        public DefaultPropertySorter(final Comparator<String> comparator) {
            this.comparator = comparator;
        }


        @Override
        public boolean isReorderRequired(final PropertyConfigurable configurable) {
            // Get the current property names
            final List<String> names = configurable.getPropertyNames();
            final List<String> sortedNames = new ArrayList<String>(names);
            Collections.sort(sortedNames, comparator);
            return !names.equals(sortedNames);
        }

        @Override
        public void sort(final PropertyConfigurable configurable) {
            final List<String> sortedNames = new ArrayList<String>(configurable.getPropertyNames());
            Collections.sort(sortedNames, comparator);
            final Map<String, ValueExpression<String>> orderedValues = new LinkedHashMap<String, ValueExpression<String>>(sortedNames.size());
            // The properties need to be reordered
            for (String name : sortedNames) {
                orderedValues.put(name, configurable.getPropertyValueExpression(name));
                // Remove the value
                configurable.removeProperty(name);
            }
            // All values should be removed and now need to be added back
            for (String name : orderedValues.keySet()) {
                final ValueExpression<String> value = orderedValues.get(name);
                configurable.setPropertyValueExpression(name, value.getValue());
            }
        }
    }
}
