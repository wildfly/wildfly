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
 * This abstract syntax node represents an approximate numeric literal.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class ASTApproximateNumericLiteral extends SimpleNode {
    //private static final String UPPER_F = "UPPER_F";
    //private static final String LOWER_F = "LOWER_F";
    //private static final String LOWER_D = "LOWER_D";
    //private static final String UPPER_D = "UPPER_D";

    //public double value;
    public String literal;

    public ASTApproximateNumericLiteral(int id) {
        super(id);
    }

    public void setValue(String number) {
        literal = number;
        /*
        // float suffix
        if(number.endsWith(LOWER_F) || number.endsWith(UPPER_F)) {
           // chop off the suffix
           number = number.substring(0, number.length()-1);
           value = Float.parseFloat(number);
        } else {
           // ends with a LOWER_D suffix, chop it off
           if(number.endsWith(LOWER_D) || number.endsWith(UPPER_D)) {
              number = number.substring(0, number.length()-1);
           }

           // regular double
           value = Double.parseDouble(number);
        }
        */
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
