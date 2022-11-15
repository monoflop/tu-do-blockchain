package com.philippkutsch.tuchain.network;

import com.google.gson.Gson;
import com.philippkutsch.tuchain.network.protocol.network.messages.PingMessage;
import com.philippkutsch.tuchain.network.protocol.Message;
import org.junit.Test;

public class MessageSerializationTest {
    @Test
    public void testGenericDeserialization() {
        String json = "{\"type\":\"ping\", \"body\":{\"timestamp\":1666630401}}";

        Gson gson = new Gson();
        Message message = gson.fromJson(json, Message.class);
        assert message.getType().equals("ping");

        PingMessage pingMessage = gson.fromJson(message.getBody(), PingMessage.class);
        assert pingMessage.getTimestamp() == 1666630401L;
    }
}
