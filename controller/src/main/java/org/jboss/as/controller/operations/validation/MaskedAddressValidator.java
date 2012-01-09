/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.validation;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates that the given parameter is a string that can be converted into a masked InetAddress.
 *
 * @author Jason T, Greene
 */
public class MaskedAddressValidator extends ModelTypeValidator {

    public MaskedAddressValidator(final boolean nullable, final boolean allowExpressions) {
        super(ModelType.STRING, nullable, allowExpressions, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            parseMasked(value);
        }
    }

    public static ParsedResult parseMasked(ModelNode value) throws OperationFailedException {
        final String[] split = value.asString().split("/");
        if (split.length != 2) {
            throw new OperationFailedException(MESSAGES.invalidAddressMaskValue(value.asString()));
        }
        try {
            // TODO - replace with non-dns routine
            InetAddress address = InetAddress.getByName(split[0]);
            int mask = Integer.parseInt(split[1]);

            int max = address.getAddress().length * 8;
            if (mask > max) {
                throw new OperationFailedException(MESSAGES.invalidAddressMask(split[1], "> " + max));
            } else if (mask < 0) {
                throw new OperationFailedException(MESSAGES.invalidAddressMask(split[1], "< 0"));
            }

            return new ParsedResult(address, mask);
        } catch (final UnknownHostException e) {
            throw new OperationFailedException(MESSAGES.invalidAddressValue(split[0], e.getLocalizedMessage()));
        } catch (final NumberFormatException e) {
            throw new OperationFailedException(MESSAGES.invalidAddressMask(split[1], e.getLocalizedMessage()));
        }
    }

    public static class ParsedResult {
        public InetAddress address;
        public int mask;

        public ParsedResult(InetAddress address, int mask) {
            this.address = address;
            this.mask = mask;
        }
    }
}
