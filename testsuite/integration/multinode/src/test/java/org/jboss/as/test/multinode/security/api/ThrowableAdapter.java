/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author bmaxwell
 *
 */
public class ThrowableAdapter extends XmlAdapter<String, Throwable> {
    private HexBinaryAdapter hexAdapter = new HexBinaryAdapter();

    @Override
    public String marshal(Throwable v) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(v);
        oos.close();
        byte[] serializedBytes = baos.toByteArray();
        return hexAdapter.marshal(serializedBytes);
    }

    @Override
    public Throwable unmarshal(String v) throws Exception {
        byte[] serializedBytes = hexAdapter.unmarshal(v);
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedBytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Throwable result = (Throwable) ois.readObject();
        return result;
    }
}