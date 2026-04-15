package com.ersted.walletservice.kafka.avro;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Kafka Deserializer for Avro SpecificRecord using Confluent wire format.
 * Resolves the target Java class from the schema's namespace+name,
 * which must match a generated SpecificRecord on the classpath.
 */
public class AvroSchemaRegistryDeserializer implements Deserializer<Object> {

    private static final byte MAGIC_BYTE = 0x00;

    private SchemaRegistryClient schemaRegistryClient;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        String url = (String) configs.get("schema.registry.url");
        schemaRegistryClient = new SchemaRegistryClient(url);
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        if (data == null) return null;

        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte magic = buffer.get();
        if (magic != MAGIC_BYTE) {
            throw new SerializationException("Unknown magic byte 0x" + Integer.toHexString(magic & 0xFF));
        }

        int schemaId = buffer.getInt();
        Schema writerSchema = schemaRegistryClient.getSchemaById(schemaId);

        try {
            @SuppressWarnings("unchecked")
            Class<SpecificRecord> clazz = (Class<SpecificRecord>) SpecificData.get().getClass(writerSchema);
            Schema readerSchema = (clazz != null) ? SpecificData.get().getSchema(clazz) : writerSchema;

            SpecificDatumReader<SpecificRecord> reader = new SpecificDatumReader<>(writerSchema, readerSchema);

            byte[] avroPayload = new byte[buffer.remaining()];
            buffer.get(avroPayload);

            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(avroPayload, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize Avro message for topic: " + topic, e);
        }
    }
}
