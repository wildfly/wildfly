/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.ejbql;

/**
 * This abstract syntax node represents an exact numeric literal.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class ASTExactNumericLiteral extends SimpleNode {
    public long value;
    public String literal;
    private static final String LOFFER_L = "l";
    private static final String UPPER_L = "L";
    private static final String OX = "0X";
    private static final String Ox = "0x";
    private static final String ZERRO = "0";

    public ASTExactNumericLiteral(int id) {
        super(id);
    }

    public void setValue(String number) {
        literal = number;

        // long suffix
        if (number.endsWith(LOFFER_L) || number.endsWith(UPPER_L)) {
            // chop off the suffix
            number = number.substring(0, number.length() - 1);
        }

        // hex
        if (number.startsWith(OX) || number.startsWith(Ox)) {
            // handle literals from 0x8000000000000000L to 0xffffffffffffffffL:
            // remove sign bit, parse as positive, then calculate the negative
            // value with the sign bit
            if (number.length() == 18) {
                byte first = Byte.decode(number.substring(0, 3)).byteValue();
                if (first >= 8) {
                    number = Ox + (first - 8) + number.substring(3);
                    value = Long.decode(number).longValue() - Long.MAX_VALUE - 1;
                    return;
                }
            }
        } else if (number.startsWith(ZERRO)) {   // octal
            // handle literals
            // from 01000000000000000000000L to 01777777777777777777777L
            // remove sign bit, parse as positive, then calculate the
            // negative value with the sign bit
            if (number.length() == 23) {
                if (number.charAt(1) == '1') {
                    number = ZERRO + number.substring(2);
                    value = Long.decode(number).longValue() - Long.MAX_VALUE - 1;
                    return;
                }
            }
        }
        value = Long.decode(number).longValue();
    }

    public String toString() {
        return literal;
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JBossQLParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
