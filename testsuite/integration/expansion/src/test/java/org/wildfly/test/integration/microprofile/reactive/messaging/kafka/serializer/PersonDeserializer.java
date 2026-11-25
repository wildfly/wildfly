/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.kafka.common.serialization.Deserializer;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PersonDeserializer implements Deserializer<Person> {
    @Override
    public Person deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
            String name = in.readUTF();
            int age = in.readInt();
            in.close();
            return new Person(name, age);
        } catch (IOException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
