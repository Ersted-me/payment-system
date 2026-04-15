package com.ersted.walletservice.kafka.avro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.avro.Schema;
import org.apache.kafka.common.errors.SerializationException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal Schema Registry HTTP client.
 * Supports {@code mock://<name>} URLs for in-process testing without a real registry.
 */
class SchemaRegistryClient {

    private static final Map<String, MockRegistry> MOCK_REGISTRIES = new ConcurrentHashMap<>();

    private final String baseUrl;
    private final boolean isMock;
    private final MockRegistry mockRegistry;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // HTTP-mode caches
    private final Map<String, Integer> subjectSchemaToId = new ConcurrentHashMap<>();
    private final Map<Integer, Schema> idToSchema = new ConcurrentHashMap<>();

    SchemaRegistryClient(String url) {
        if (url != null && url.startsWith("mock://")) {
            this.isMock = true;
            String mockName = url.substring("mock://".length());
            this.mockRegistry = MOCK_REGISTRIES.computeIfAbsent(mockName, k -> new MockRegistry());
            this.baseUrl = null;
            this.httpClient = null;
        } else {
            this.isMock = false;
            this.mockRegistry = null;
            this.baseUrl = url;
            this.httpClient = HttpClient.newHttpClient();
        }
    }

    int registerSchema(String subject, Schema schema) {
        String schemaStr = schema.toString();
        String cacheKey = subject + ":" + schemaStr;

        if (isMock) {
            return mockRegistry.register(subject, schemaStr);
        }

        return subjectSchemaToId.computeIfAbsent(cacheKey, k -> {
            try {
                ObjectNode body = objectMapper.createObjectNode();
                body.put("schema", schemaStr);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/subjects/" + subject + "/versions"))
                        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode node = objectMapper.readTree(response.body());
                return node.get("id").asInt();
            } catch (IOException | InterruptedException e) {
                throw new SerializationException("Failed to register schema for subject: " + subject, e);
            }
        });
    }

    Schema getSchemaById(int id) {
        if (isMock) {
            return mockRegistry.getById(id);
        }

        return idToSchema.computeIfAbsent(id, k -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/schemas/ids/" + id))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode node = objectMapper.readTree(response.body());
                return new Schema.Parser().parse(node.get("schema").asText());
            } catch (IOException | InterruptedException e) {
                throw new SerializationException("Failed to retrieve schema for id: " + id, e);
            }
        });
    }

    private static class MockRegistry {
        private final Map<String, Integer> schemaToId = new ConcurrentHashMap<>();
        private final Map<Integer, Schema> idToSchema = new ConcurrentHashMap<>();
        private final AtomicInteger idSequence = new AtomicInteger(1);

        int register(String subject, String schemaStr) {
            return schemaToId.computeIfAbsent(subject + ":" + schemaStr, k -> {
                int id = idSequence.getAndIncrement();
                idToSchema.put(id, new Schema.Parser().parse(schemaStr));
                return id;
            });
        }

        Schema getById(int id) {
            Schema schema = idToSchema.get(id);
            if (schema == null) {
                throw new SerializationException("Schema not found in mock registry for id: " + id);
            }
            return schema;
        }
    }
}
