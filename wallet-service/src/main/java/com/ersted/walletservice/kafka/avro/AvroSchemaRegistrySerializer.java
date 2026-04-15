package com.ersted.walletservice.kafka.avro;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Kafka Serializer for Avro SpecificRecord using Confluent wire format:
 * {@code 0x00 | 4-byte schema-id (big-endian) | avro-binary-payload}
 */
public class AvroSchemaRegistrySerializer implements Serializer<Object> {

    private static final byte MAGIC_BYTE = 0x00;

    private SchemaRegistryClient schemaRegistryClient;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        String url = (String) configs.get("schema.registry.url");
        schemaRegistryClient = new SchemaRegistryClient(url);
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) return null;

        SpecificRecord record = (SpecificRecord) data;
        Schema schema = record.getSchema();
        String subject = topic + "-value";

        try {
            int schemaId = schemaRegistryClient.registerSchema(subject, schema);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(MAGIC_BYTE);
            out.write(ByteBuffer.allocate(4).putInt(schemaId).array());

            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            new SpecificDatumWriter<SpecificRecord>(schema).write(record, encoder);
            encoder.flush();

            return out.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize Avro record for topic: " + topic, e);
        }
    }
}
