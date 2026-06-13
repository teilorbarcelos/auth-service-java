package com.app.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQMessage;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RabbitMQProviderTest {

    private RabbitMQProvider provider;
    private RabbitMQClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        client = mock(RabbitMQClient.class);
        objectMapper = new ObjectMapper();
        provider = new RabbitMQProvider();
        Instance<RabbitMQClient> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.get()).thenReturn(client);
        provider.setClient(instance);
        provider.objectMapper = objectMapper;
        provider.enabled = true;
    }

    @Test
    void testSetClient_NotResolvable() {
        RabbitMQProvider p = new RabbitMQProvider();
        Instance<RabbitMQClient> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(false);
        assertDoesNotThrow(() -> p.setClient(instance));
        assertFalse(p.isConnected());
    }

    @Test
    void testIsEnabled() {
        assertTrue(provider.isEnabled());
        provider.enabled = false;
        assertFalse(provider.isEnabled());
    }

    @Test
    void testPublishSuccess() {
        Map<String, Object> message = Map.of("key", "value");
        when(client.basicPublish(anyString(), anyString(), any(Buffer.class)))
                .thenReturn(Future.succeededFuture());

        provider.publish("test-queue", message);

        verify(client).basicPublish(eq(""), eq("test-queue"), any(Buffer.class));
    }

    @Test
    void testPublishDisabled() {
        provider.enabled = false;

        provider.publish("test-queue", Map.of());

        verify(client, never()).basicPublish(anyString(), anyString(), any());
    }

    @Test
    void testPublishError() {
        when(client.basicPublish(anyString(), anyString(), any(Buffer.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("Broken")));

        assertDoesNotThrow(() -> provider.publish("test-queue", Map.of("k", "v")));
    }

    @Test
    void testPublishSerializationError() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        provider.objectMapper = failingMapper;
        when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

        assertDoesNotThrow(() -> provider.publish("test-queue", Map.of("k", "v")));
    }

    @Test
    void testSubscribeDisabled() {
        provider.enabled = false;

        provider.subscribe("test-queue", msg -> {});

        verify(client, never()).basicConsumer(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSubscribeSuccess() {
        RabbitMQConsumer consumer = mock(RabbitMQConsumer.class);
        RabbitMQMessage msg = mock(RabbitMQMessage.class);
        Buffer buffer = Buffer.buffer("{\"foo\":\"bar\"}");
        when(msg.body()).thenReturn(buffer);
        when(client.basicConsumer(anyString())).thenReturn(Future.succeededFuture(consumer));

        provider.subscribe("test-queue", m -> {
            assertEquals("bar", m.get("foo"));
        });

        verify(client).basicConsumer("test-queue");

        ArgumentCaptor<Handler<RabbitMQMessage>> msgHandler = ArgumentCaptor.forClass(Handler.class);
        verify(consumer).handler(msgHandler.capture());

        ArgumentCaptor<Handler<Void>> endHandler = ArgumentCaptor.forClass(Handler.class);
        verify(consumer).endHandler(endHandler.capture());

        ArgumentCaptor<Handler<Throwable>> exHandler = ArgumentCaptor.forClass(Handler.class);
        verify(consumer).exceptionHandler(exHandler.capture());

        msgHandler.getValue().handle(msg);
        endHandler.getValue().handle(null);
        exHandler.getValue().handle(new RuntimeException("test error"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSubscribeMessageParsingError() {
        RabbitMQConsumer consumer = mock(RabbitMQConsumer.class);
        RabbitMQMessage msg = mock(RabbitMQMessage.class);
        Buffer buffer = Buffer.buffer("invalid json");
        when(msg.body()).thenReturn(buffer);
        when(client.basicConsumer(anyString())).thenReturn(Future.succeededFuture(consumer));

        provider.subscribe("test-queue", m -> fail("Should not be called"));

        ArgumentCaptor<Handler<RabbitMQMessage>> msgHandler = ArgumentCaptor.forClass(Handler.class);
        verify(consumer).handler(msgHandler.capture());

        assertDoesNotThrow(() -> msgHandler.getValue().handle(msg));
    }

    @Test
    void testSubscribeError() {
        when(client.basicConsumer(anyString()))
                .thenReturn(Future.failedFuture(new RuntimeException("Consumer failed")));

        assertDoesNotThrow(() -> provider.subscribe("test-queue", msg -> {}));
    }

    @Test
    void testIsConnected() {
        assertTrue(provider.isConnected());

        provider.enabled = false;
        assertFalse(provider.isConnected());

        RabbitMQProvider p2 = new RabbitMQProvider();
        p2.enabled = true;
        assertFalse(p2.isConnected());

        p2.enabled = false;
        assertFalse(p2.isConnected());
    }
}
