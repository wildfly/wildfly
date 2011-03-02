package org.jboss.as.server.deployment;

/**
 * An immutable, type-safe object attachment key.  Such a key has no value outside of its object identity.
 *
 * @param <T> the attachment type
 */
public abstract class AttachmentKey<T> {

    AttachmentKey() {
    }

    /**
     * Cast the value to the type of this attachment key.
     *
     * @param value the value
     * @return the cast value
     */
    public abstract T cast(Object value);

    /**
     * Construct a new simple attachment key.
     *
     * @param valueClass the value class
     * @param <T> the attachment type
     * @return the new instance
     */
    public static <T> AttachmentKey<T> create(final Class<? super T> valueClass) {
        return new SimpleAttachmentKey(valueClass);
    }

    /**
     * Construct a new list attachment key.
     *
     * @param valueClass the list value class
     * @param <T> the list value type
     * @return the new instance
     */
    @SuppressWarnings("unchecked")
    public static <T> AttachmentKey<AttachmentList<T>> createList(final Class<? super T> valueClass) {
        return new ListAttachmentKey(valueClass);
    }
}

class ListAttachmentKey<T> extends AttachmentKey<AttachmentList<T>> {

    private final Class<T> valueClass;

    ListAttachmentKey(final Class<T> valueClass) {
        this.valueClass = valueClass;
    }

    @SuppressWarnings({ "unchecked" })
    public AttachmentList<T> cast(final Object value) {
        if (value == null) {
            return null;
        }
        AttachmentList<?> list = (AttachmentList<?>) value;
        final Class<?> listValueClass = list.getValueClass();
        if (listValueClass != valueClass) {
            throw new ClassCastException();
        }
        return (AttachmentList<T>) list;
    }

    Class<T> getValueClass() {
        return valueClass;
    }
}