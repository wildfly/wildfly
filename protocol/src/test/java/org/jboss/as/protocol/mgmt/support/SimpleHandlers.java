/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.protocol.mgmt.support;

import java.io.DataInput;
import java.io.IOException;

import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementResponseHandler;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SimpleHandlers {

    public static final byte SIMPLE_REQUEST = 102;
    public static final byte REQUEST_WITH_NO_HANDLER = 103;
    public static final byte REQUEST_WITH_BAD_READ = 104;
    public static final byte REQUEST_WITH_BAD_WRITE = 105;

    public static class Request extends ManagementRequest<Integer>{
        final int sentData;
        final byte requestCode;

        public Request(byte requestCode, int sentData) {
            this.requestCode = requestCode;
            this.sentData = sentData;
        }

        @Override
        protected byte getRequestCode() {
            return requestCode;
        }

        @Override
        protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
            //System.out.println("Writing request");
            output.writeInt(sentData);
        }

        @Override
        protected ManagementResponseHandler<Integer> getResponseHandler() {
            return new ManagementResponseHandler<Integer>() {
                protected Integer readResponse(DataInput input) throws IOException {
                    int i = input.readInt();
                    return i;
                }
            };
        }
    }

    public static class OperationHandler implements ManagementOperationHandler {

        @Override
        public ManagementRequestHandler getRequestHandler(byte id) {
            switch (id) {
            case SIMPLE_REQUEST:
                return new RequestHandler();
            case REQUEST_WITH_BAD_READ:
                return new BadReadRequestHandler();
            case REQUEST_WITH_BAD_WRITE:
                return new BadWriteRequestHandler();
            case REQUEST_WITH_NO_HANDLER:
                //No handler for this
            default:
                return null;

            }
        }
    }

    public static class RequestHandler extends ManagementRequestHandler {
        int data;

        @Override
        public void readRequest(DataInput input) throws IOException {
            data = input.readInt();
        }

        @Override
        public void writeResponse(FlushableDataOutput output) throws IOException {
            output.writeInt(data * 2);
        }
    }

    public static class BadReadRequestHandler extends ManagementRequestHandler {

        @Override
        public void readRequest(DataInput input) throws IOException {
            throw new IOException("BadReadRequest");
        }
    }

    public static class BadWriteRequestHandler extends ManagementRequestHandler {

        @Override
        public void readRequest(DataInput input) throws IOException {
            int data = input.readInt();
        }

        @Override
        public void writeResponse(FlushableDataOutput output) throws IOException {
            throw new IOException("BadWriteRequest");
        }
    }

}
