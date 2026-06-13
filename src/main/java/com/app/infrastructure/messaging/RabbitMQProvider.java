package com.app.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.rabbitmq.RabbitMQClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.function.Consumer;

@ApplicationScoped
public class RabbitMQProvider {

    private static final Logger LOG = Logger.getLogger(RabbitMQProvider.class);

    @ConfigProperty(name = "app.messaging.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    ObjectMapper objectMapper;

    private RabbitMQClient client;

    @Inject
    void setClient(Instance<RabbitMQClient> clientInstance) {
        if (clientInstance.isResolvable()) {
            this.client = clientInstance.get();
        }
    }

    public void publish(String queueName, Map<String, Object> message) {
        if (!enabled) {
            return;
        }

        try {
            String body = objectMapper.writeValueAsString(message);
            client.basicPublish("", queueName, io.vertx.core.buffer.Buffer.buffer(body))
                    .onFailure(t -> LOG.errorv("[RabbitMQ] Publish failed: {0}", t.getMessage()));
        } catch (Exception e) {
            LOG.errorv("[RabbitMQ] Publish error: {0}", e.getMessage());
        }
    }

    public void subscribe(String queueName, Consumer<Map<String, Object>> callback) {
        if (!enabled) {
            return;
        }

        client.basicConsumer(queueName)
                .onFailure(t -> LOG.errorv("[RabbitMQ] Subscribe failed: {0}", t.getMessage()))
                .onSuccess(consumer -> {
                    consumer.handler(msg -> {
                        try {
                            String body = msg.body().toString();
                            Map<String, Object> content = objectMapper.readValue(body, Map.class);
                            callback.accept(content);
                        } catch (Exception e) {
                            LOG.errorv("[RabbitMQ] Error processing message: {0}", e.getMessage());
                        }
                    });
                    consumer.endHandler(v -> LOG.warn("[RabbitMQ] Consumer ended"));
                    consumer.exceptionHandler(t -> LOG.errorv("[RabbitMQ] Consumer error: {0}", t.getMessage()));
                });
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConnected() {
        return enabled && client != null;
    }
}
