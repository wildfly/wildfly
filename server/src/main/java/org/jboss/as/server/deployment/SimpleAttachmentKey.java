package org.jboss.as.server.deployment;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SimpleAttachmentKey<T> extends AttachmentKey<T> {
    private final Class<T> valueClass;

    SimpleAttachmentKey(final Class<T> valueClass) {
        this.valueClass = valueClass;
    }

    public T cast(final Object value) {
        return valueClass.cast(value);
    }

    @Override
    public String toString() {
        if (valueClass != null) {
            StringBuilder sb = new StringBuilder(getClass().getName());
            sb.append("<");
            sb.append(valueClass.getName());
            sb.append(">");
            return sb.toString();
        }
        return super.toString();
    }
}
