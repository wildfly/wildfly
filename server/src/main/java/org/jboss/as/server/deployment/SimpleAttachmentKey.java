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
}
