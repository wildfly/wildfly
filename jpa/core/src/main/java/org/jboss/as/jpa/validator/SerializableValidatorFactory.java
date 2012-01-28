/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.validator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

/**
 * Serializable validator factory
 * <p/>
 * TODO:  clustering support is needed (readResolve should set delegate to already initialized validator factory)
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author Scott Marlow
 * @version $Revision: $
 */
public class SerializableValidatorFactory implements ValidatorFactory, Serializable {
    /**
     * Serial version uid
     */
    private static final long serialVersionUID = 1043124L;

    /**
     * The validator factory that all invocations are delegated to.
     */
    private transient volatile ValidatorFactory delegate = new JPALazyValidatorFactory();

    public static ValidatorFactory validatorFactory() {
        return new SerializableValidatorFactory();
    }

    /**
     * Get the message interpolator
     *
     * @return The interpolator
     */
    public MessageInterpolator getMessageInterpolator() {
        return delegate.getMessageInterpolator();
    }


    /**
     * Get the validator
     *
     * @return The validator
     */
    public Validator getValidator() {
        return delegate.getValidator();
    }

    /**
     * Get the validator context
     *
     * @return The context
     */
    public ValidatorContext usingContext() {
        return delegate.usingContext();
    }

    /**
     * Unwrap
     *
     * @param type The type
     * @return The context
     */
    public <T> T unwrap(Class<T> type) {
        return delegate.unwrap(type);
    }

    /**
     * Get the constraint validator factory
     *
     * @return The factory
     */
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return delegate.getConstraintValidatorFactory();
    }

    /**
     * Get the traversable resolver
     *
     * @return The resolver
     */
    public TraversableResolver getTraversableResolver() {
        return delegate.getTraversableResolver();
    }

    /**
     * Write the object - Nothing is written as the validator factory is already known
     *
     * @param out The output stream
     * @throws IOException Thrown if an error occurs
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /**
     * Read the object - Nothing is read as the validator factory is already known
     *
     * @param in The output stream
     * @throws IOException Thrown if an error occurs
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // create a new validator factory (when HV-460 is fixed, we should be able to reuse th HV factory)
        delegate = new JPALazyValidatorFactory();
    }

}
