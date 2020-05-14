/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wildfly.clustering.ee.cache.scheduler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * A concurrent deque that allows direct item removal without traversal.
 *
 * @author Jason T. Greene
 * @author Paul Ferraro
 */
public interface ConcurrentDirectDeque<E> extends Deque<E> {

    static <K> ConcurrentDirectDeque<K> newInstance() {
        return FACTORY.get();
    }

    @SuppressWarnings("rawtypes")
    Supplier<ConcurrentDirectDeque> FACTORY = new Supplier<ConcurrentDirectDeque>() {
        private final Constructor<? extends ConcurrentDirectDeque> constructor = findConstructor();

        private Constructor<? extends ConcurrentDirectDeque> findConstructor() {
            Class<? extends ConcurrentDirectDeque> queueClass = PortableConcurrentDirectDeque.class;
            try {
                queueClass = new FastConcurrentDirectDeque().getClass();
            } catch (Throwable e) {
                // If sun.misc.Unsafe is unavailable
            }
            try {
                return queueClass.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            }
        }

        @Override
        public ConcurrentDirectDeque get() {
            try {
                return this.constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Equivalent to {@link #offerFirst(Object)}, but returns a token used for fast removal.
     * @param e the element to offer
     * @return a token suitable for use by {@link #remove(Object)}
     */
    Object offerFirstAndReturnToken(E e);

    /**
     * Equivalent to {@link #offerLast(Object)}, but returns a token used for fast removal.
     * @param e the element to offer
     * @return a token suitable for use by {@link #remove(Object)}
     */
    Object offerLastAndReturnToken(E e);

    /**
     * Removes the element associated with the given token.
     * @param token the token returned via {@link #offerFirstAndReturnToken(Object)} or {@link #offerLastAndReturnToken(Object)}.
     */
    void removeToken(Object token);

    // Delegate collection methods to deque methods

    @Override
    default boolean add(E e) {
        return this.offerLast(e);
    }

    @Override
    default boolean remove(Object o) {
        return this.removeFirstOccurrence(o);
    }

    // Delegate stack methods to deque methods

    @Override
    default E peek() {
        return this.peekFirst();
    }

    @Override
    default E pop() {
        return this.removeFirst();
    }

    @Override
    default void push(E e) {
        this.addFirst(e);
    }

    // Delegate queue methods to deque methods

    @Override
    default E element() {
        return this.getFirst();
    }

    @Override
    default boolean offer(E e) {
        return this.offerLast(e);
    }

    @Override
    default E poll() {
        return this.pollFirst();
    }

    @Override
    default E remove() {
        return this.removeFirst();
    }
}
