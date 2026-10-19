package io.kestra.plugin.amqp.models;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.LongStringHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class MessageTest {

    @Test
    void shouldExtractRoutingKeyFromEnvelope() throws Exception {
        Envelope envelope = new Envelope(1L, false, "my.exchange", "order.created");
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().build();

        Message message = Message.of("hello".getBytes(), SerdeType.STRING, properties, envelope);

        assertThat(message.getRoutingKey(), is("order.created"));
        assertThat(message.getData(), is("hello"));
    }

    @Test
    void shouldHandleNullEnvelopeGracefully() throws Exception {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().build();

        Message message = Message.of("hello".getBytes(), SerdeType.STRING, properties, null);

        assertThat(message.getRoutingKey(), is(nullValue()));
    }

    @Test
    void shouldKeepHeaderValuesWithTheirTypes() throws Exception {
        Date time = new Date(1_760_889_996_000L);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .headers(Map.of(
                "retry", 3,
                "source", LongStringHelper.asLongString("billing"),
                "x-death", List.of(Map.of(
                    "count", 1L,
                    "reason", LongStringHelper.asLongString("expired"),
                    "routing-keys", List.of(LongStringHelper.asLongString("orders.wait")),
                    "time", time
                ))
            ))
            .build();

        Message message = Message.of("hello".getBytes(), SerdeType.STRING, properties, null);

        assertThat(message.getHeaders(), is(Map.of(
            "retry", 3,
            "source", "billing",
            "x-death", List.of(Map.of(
                "count", 1L,
                "reason", "expired",
                "routing-keys", List.of("orders.wait"),
                "time", time.toInstant()
            ))
        )));
    }
}
