package org.db;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ProtocolSocketTest {

    private static String send(String line) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 2001);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer.write(line);
            writer.write("\n");
            writer.flush();
            return reader.readLine();
        }
    }

    private static List<String> sendAll(String... lines) throws IOException {
        List<String> responses = new ArrayList<>();
        try (Socket socket = new Socket("127.0.0.1", 2001);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
                writer.flush();
                responses.add(reader.readLine());
            }
        }
        return responses;
    }

    @Test
    void spacedValueRoundTrips() throws IOException {
        List<String> response =  ProtocolSocketTest.sendAll("SET KEY VALUE IS GREAT", "GET KEY");
        assert "VALUE IS GREAT".equals(response.get(1));
    }

    @Test
    void doubleSpaceRejected() throws IOException {
        assert "Invalid command".equals(ProtocolSocketTest.send("GET  A  B"));
    }

    @Test
    void getWithExtraArgsRejected() throws IOException {
       assert "GET does not accept a value".equals(ProtocolSocketTest.send("GET A B"));
    }
}
