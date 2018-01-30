/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @build DummyWebSocketServer
 * @run testng/othervm -Djdk.httpclient.HttpClient.log=trace Exceptionally
 */

import jdk.incubator.http.WebSocket;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static jdk.incubator.http.HttpClient.newHttpClient;
import static jdk.incubator.http.WebSocket.NORMAL_CLOSURE;
import static org.testng.Assert.assertThrows;

public class Exceptionally {

    private static final Class<NullPointerException> NPE
            = NullPointerException.class;
    private static final Class<IllegalArgumentException> IAE
            = IllegalArgumentException.class;

    @Test
    public void testNull() throws IOException {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();

            assertThrows(NPE, () -> ws.sendText(null, false));
            assertThrows(NPE, () -> ws.sendText(null, true));
            assertThrows(NPE, () -> ws.sendBinary(null, false));
            assertThrows(NPE, () -> ws.sendBinary(null, true));
            assertThrows(NPE, () -> ws.sendPing(null));
            assertThrows(NPE, () -> ws.sendPong(null));
            assertThrows(NPE, () -> ws.sendClose(NORMAL_CLOSURE, null));
        }
    }

    @Test
    public void testIllegalArgument() throws IOException {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();

            assertIAE(ws.sendPing(ByteBuffer.allocate(126)));
            assertIAE(ws.sendPing(ByteBuffer.allocate(127)));
            assertIAE(ws.sendPing(ByteBuffer.allocate(128)));
            assertIAE(ws.sendPing(ByteBuffer.allocate(129)));
            assertIAE(ws.sendPing(ByteBuffer.allocate(256)));

            assertIAE(ws.sendPong(ByteBuffer.allocate(126)));
            assertIAE(ws.sendPong(ByteBuffer.allocate(127)));
            assertIAE(ws.sendPong(ByteBuffer.allocate(128)));
            assertIAE(ws.sendPong(ByteBuffer.allocate(129)));
            assertIAE(ws.sendPong(ByteBuffer.allocate(256)));

            assertIAE(ws.sendText(incompleteString(), true));
            assertIAE(ws.sendText(incompleteString(), false));
            assertIAE(ws.sendText(malformedString(), true));
            assertIAE(ws.sendText(malformedString(), false));

            assertIAE(ws.sendClose(NORMAL_CLOSURE, stringWithNBytes(124)));
            assertIAE(ws.sendClose(NORMAL_CLOSURE, stringWithNBytes(125)));
            assertIAE(ws.sendClose(NORMAL_CLOSURE, stringWithNBytes(128)));
            assertIAE(ws.sendClose(NORMAL_CLOSURE, stringWithNBytes(256)));
            assertIAE(ws.sendClose(NORMAL_CLOSURE, stringWithNBytes(257)));
            assertIAE(ws.sendClose(NORMAL_CLOSURE, stringWith2NBytes((123 / 2) + 1)));
            assertIAE(ws.sendClose(NORMAL_CLOSURE, malformedString()));
            assertIAE(ws.sendClose(NORMAL_CLOSURE, incompleteString()));

            assertIAE(ws.sendClose(-2, "a reason"));
            assertIAE(ws.sendClose(-1, "a reason"));
            assertIAE(ws.sendClose(0, "a reason"));
            assertIAE(ws.sendClose(1, "a reason"));
            assertIAE(ws.sendClose(500, "a reason"));
            assertIAE(ws.sendClose(998, "a reason"));
            assertIAE(ws.sendClose(999, "a reason"));
            assertIAE(ws.sendClose(1002, "a reason"));
            assertIAE(ws.sendClose(1003, "a reason"));
            assertIAE(ws.sendClose(1006, "a reason"));
            assertIAE(ws.sendClose(1007, "a reason"));
            assertIAE(ws.sendClose(1009, "a reason"));
            assertIAE(ws.sendClose(1010, "a reason"));
            assertIAE(ws.sendClose(1012, "a reason"));
            assertIAE(ws.sendClose(1013, "a reason"));
            assertIAE(ws.sendClose(1015, "a reason"));
            assertIAE(ws.sendClose(5000, "a reason"));
            assertIAE(ws.sendClose(32768, "a reason"));
            assertIAE(ws.sendClose(65535, "a reason"));
            assertIAE(ws.sendClose(65536, "a reason"));
            assertIAE(ws.sendClose(Integer.MAX_VALUE, "a reason"));
            assertIAE(ws.sendClose(Integer.MIN_VALUE, "a reason"));

            assertThrows(IAE, () -> ws.request(Integer.MIN_VALUE));
            assertThrows(IAE, () -> ws.request(-1));
            assertThrows(IAE, () -> ws.request(0));
        }
    }

    @Test
    public void testIllegalStateOutstanding1() throws Exception {
        try (DummyWebSocketServer server = notReadingServer()) {
            server.open();
            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();

            ByteBuffer data = ByteBuffer.allocate(65536);
            for (int i = 0; ; i++) {
                System.out.println("cycle #" + i);
                try {
                    ws.sendBinary(data, true).get(10, TimeUnit.SECONDS);
                    data.clear();
                } catch (TimeoutException e) {
                    break;
                }
            }
            assertISE(ws.sendBinary(ByteBuffer.allocate(0), true));
            assertISE(ws.sendText("", true));
        }
    }

    @Test
    public void testIllegalStateOutstanding2() throws Exception {
        try (DummyWebSocketServer server = notReadingServer()) {
            server.open();
            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();

            CharBuffer data = CharBuffer.allocate(65536);
            for (int i = 0; ; i++) {
                System.out.println("cycle #" + i);
                try {
                    ws.sendText(data, true).get(10, TimeUnit.SECONDS);
                    data.clear();
                } catch (TimeoutException e) {
                    break;
                }
            }
            assertISE(ws.sendText("", true));
            assertISE(ws.sendBinary(ByteBuffer.allocate(0), true));
        }
    }

    private static DummyWebSocketServer notReadingServer() {
        return new DummyWebSocketServer() {
            @Override
            protected void serve(SocketChannel channel) throws IOException {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    @Test
    public void testIllegalStateIntermixed1() throws IOException {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();

            ws.sendBinary(ByteBuffer.allocate(16), false).join();
            assertISE(ws.sendText("text", false));
            assertISE(ws.sendText("text", true));
        }
    }

    @Test
    public void testIllegalStateIntermixed2() throws IOException {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();

            ws.sendText("text", false).join();
            assertISE(ws.sendBinary(ByteBuffer.allocate(16), false));
            assertISE(ws.sendBinary(ByteBuffer.allocate(16), true));
        }
    }

    private static String malformedString() {
        return new String(new char[]{0xDC00, 0xD800});
    }

    private static String incompleteString() {
        return new String(new char[]{0xD800});
    }

    private static String stringWithNBytes(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append("A");
        }
        return sb.toString();
    }

    private static String stringWith2NBytes(int n) {
        // Russian alphabet repeated cyclically
        char FIRST = '\u0410';
        char LAST = '\u042F';
        StringBuilder sb = new StringBuilder(n);
        char c = FIRST;
        for (int i = 0; i < n; i++) {
            if (++c > LAST) {
                c = FIRST;
            }
            sb.append(c);
        }
        String s = sb.toString();
        assert s.length() == n && s.getBytes(StandardCharsets.UTF_8).length == 2 * n;
        return s;
    }

    @Test
    public void testIllegalStateSendClose() throws IOException {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();

            ws.sendClose(NORMAL_CLOSURE, "normal close").join();

            assertISE(ws.sendText("", true));
            assertISE(ws.sendText("", false));
            assertISE(ws.sendText("abc", true));
            assertISE(ws.sendText("abc", false));
            assertISE(ws.sendBinary(ByteBuffer.allocate(0), true));
            assertISE(ws.sendBinary(ByteBuffer.allocate(0), false));
            assertISE(ws.sendBinary(ByteBuffer.allocate(1), true));
            assertISE(ws.sendBinary(ByteBuffer.allocate(1), false));

            assertISE(ws.sendPing(ByteBuffer.allocate(125)));
            assertISE(ws.sendPing(ByteBuffer.allocate(124)));
            assertISE(ws.sendPing(ByteBuffer.allocate(1)));
            assertISE(ws.sendPing(ByteBuffer.allocate(0)));

            assertISE(ws.sendPong(ByteBuffer.allocate(125)));
            assertISE(ws.sendPong(ByteBuffer.allocate(124)));
            assertISE(ws.sendPong(ByteBuffer.allocate(1)));
            assertISE(ws.sendPong(ByteBuffer.allocate(0)));
        }
    }

    private static void assertIAE(CompletableFuture<?> stage) {
        assertExceptionally(IAE, stage);
    }

    private static void assertISE(CompletableFuture<?> stage) {
        assertExceptionally(IllegalStateException.class, stage);
    }

    private static void assertExceptionally(Class<? extends Throwable> clazz,
                                            CompletableFuture<?> stage) {
        stage.handle((result, error) -> {
            if (error instanceof CompletionException) {
                Throwable cause = error.getCause();
                if (cause == null) {
                    throw new AssertionError("Unexpected null cause: " + error);
                }
                assertException(clazz, cause);
            } else {
                assertException(clazz, error);
            }
            return null;
        }).join();
    }

    private static void assertException(Class<? extends Throwable> clazz,
                                        Throwable t) {
        if (t == null) {
            throw new AssertionError("Expected " + clazz + ", caught nothing");
        }
        if (!clazz.isInstance(t)) {
            throw new AssertionError("Expected " + clazz + ", caught " + t);
        }
    }
}
