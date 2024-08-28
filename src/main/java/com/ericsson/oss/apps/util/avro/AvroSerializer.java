/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.util.avro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    @Override
    public void close() {
        // No configuration required
    }

    @Override
    public void configure(final Map<String, ?> arg0, final boolean arg1) {
        // No configuration required
    }

    @Override
    public byte[] serialize(final String topic, final T data) {
        log.debug("data to serialize='{}'", data);

        if (Objects.isNull(data)) {
            return new byte[0];
        }

        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            final BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(byteArrayOutputStream, null);

            final DatumWriter<GenericRecord> datumWriter = new SpecificDatumWriter<>(data.getSchema());
            datumWriter.write(data, binaryEncoder);

            binaryEncoder.flush();

            final byte[] avroData = byteArrayOutputStream.toByteArray();
            final byte[] header = magicByte();
            final byte[] avroDataWithHeader = new byte[header.length + avroData.length];
            System.arraycopy(header, 0, avroDataWithHeader, 0, header.length);
            System.arraycopy(avroData, 0, avroDataWithHeader, 5, avroData.length);

            return avroDataWithHeader;
        } catch (final IOException e) {
            log.warn("Can't serialize data='{}' for topic='{}'", data, topic, e);
            return new byte[0];
        }
    }

    private byte[] magicByte() {
        final byte magicByte = (byte) 0;
        final byte[] header = new byte[5];
        header[0] = magicByte;
        return header;
    }
}
