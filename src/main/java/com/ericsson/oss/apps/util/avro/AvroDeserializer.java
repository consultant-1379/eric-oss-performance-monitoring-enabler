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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private static final int MAGIC_BYTE = 0;

    protected final Class<T> targetType;

    public AvroDeserializer(final Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public void configure(final Map<String, ?> arg0, final boolean arg1) {
        // No configuration required
    }

    @Override
    public T deserialize(final String topic, final byte[] data) {
        try {
            T result = null;
            if (data != null) {
                final DatumReader<T> datumReader = new SpecificDatumReader<>(targetType.getDeclaredConstructor().newInstance().getSchema());
                final Decoder decoder = DecoderFactory.get().binaryDecoder(getAvroData(data), null);
                result = datumReader.read(null, decoder);
            }
            return result;
        } catch (final Exception e) {
            log.warn("Unable to deserialize message from monitoring objects topic", e);
            return null;
        }
    }

    private byte[] getAvroData(final byte[] data) throws IOException {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            log.debug("Number of bytes available in Kafka message: {}", inputStream.available());
            final int magicByte = inputStream.read();

            if (MAGIC_BYTE == magicByte) {
                ByteBuffer.wrap(inputStream.readNBytes(4)).getInt();
                return inputStream.readAllBytes();
            }
            return data;
        }
    }
}