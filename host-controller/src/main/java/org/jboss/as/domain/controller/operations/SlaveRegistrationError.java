/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations;

/**
 * Used to propogate error codes as part of the error message when an error occurs registering the a slave host controller.
 * I do not want to modify the protocol at the moment to support error codes natively.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SlaveRegistrationError {

    private static final String SEPARATOR = "-$-";

    private final ErrorCode errorCode;
    private final String errorMessage;

    private SlaveRegistrationError(ErrorCode errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static SlaveRegistrationError parse(String raw) {
        int index = raw.indexOf("-$-");
        if (index == -1) {
            return new SlaveRegistrationError(ErrorCode.NONE, raw);
        }

        ErrorCode code = ErrorCode.parseCode(Integer.valueOf(raw.substring(0, index)));
        String msg = raw.substring(index + SEPARATOR.length());
        return new SlaveRegistrationError(code, msg);
    }

    public static String formatHostAlreadyExists(String msg) {
        return new SlaveRegistrationError(ErrorCode.HOST_ALREADY_EXISTS, msg).toString();
    }

    public String toString() {
        return errorCode.getCode() + SEPARATOR + errorMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public enum ErrorCode {
        NONE(0),
        HOST_ALREADY_EXISTS(1);

        private final int code;

        ErrorCode(int code){
            this.code = code;
        }

        int getCode() {
            return code;
        }

        static ErrorCode parseCode(int code) {
            if (code == NONE.getCode()) {
                return NONE;
            } else if (code == HOST_ALREADY_EXISTS.getCode()) {
                return HOST_ALREADY_EXISTS;
            }
            throw new IllegalArgumentException("Invalid code " + code);
        }
    }
}
