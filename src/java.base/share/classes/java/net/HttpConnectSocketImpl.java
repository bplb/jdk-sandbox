/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import sun.nio.ch.NioSocketImpl;

/**
 * Basic SocketImpl that relies on the internal HTTP protocol handler
 * implementation to perform the HTTP tunneling and authentication. The
 * sockets impl is swapped out and replaced with the socket from the HTTP
 * handler after the tunnel is successfully setup.
 *
 * @since 1.8
 */

/*package*/ class HttpConnectSocketImpl extends SocketImpl implements SocketImpl.DelegatingImpl {

    private static final String httpURLClazzStr =
                                  "sun.net.www.protocol.http.HttpURLConnection";
    private static final String netClientClazzStr = "sun.net.NetworkClient";
    private static final String doTunnelingStr = "doTunneling";
    private static final Field httpField;
    private static final Field serverSocketField;
    private static final Method doTunneling;

    private final String server;
    private InetSocketAddress external_address;
    private HashMap<Integer, Object> optionsMap = new HashMap<>();
    private final SocketImpl delegate;

    static  {
        try {
            Class<?> httpClazz = Class.forName(httpURLClazzStr, true, null);
            httpField = httpClazz.getDeclaredField("http");
            doTunneling = httpClazz.getDeclaredMethod(doTunnelingStr);
            Class<?> netClientClazz = Class.forName(netClientClazzStr, true, null);
            serverSocketField = netClientClazz.getDeclaredField("serverSocket");

            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<>() {
                    public Void run() {
                        httpField.setAccessible(true);
                        serverSocketField.setAccessible(true);
                        return null;
                }
            });
        } catch (ReflectiveOperationException x) {
            throw new InternalError("Should not reach here", x);
        }
    }
    HttpConnectSocketImpl(SocketImpl delegate) {
        this.delegate = delegate;
        this.server = null;
        throw new InternalError();
    }

    HttpConnectSocketImpl(Proxy proxy, SocketImpl delegate) {
        this.delegate = delegate;
        SocketAddress a = proxy.address();
        if ( !(a instanceof InetSocketAddress) )
            throw new IllegalArgumentException("Unsupported address type");

        InetSocketAddress ad = (InetSocketAddress) a;
        server = ad.getHostString();
        port = ad.getPort();
    }

    @Override
    protected void create(boolean stream) throws IOException {
        delegate.create(stream);
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        connect(new InetSocketAddress(host, port), 0);
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        connect(new InetSocketAddress(address, port), 0);
    }

    @Override
    void setSocket(Socket socket) {
        delegate.socket = socket;
        super.setSocket(socket);
    }

    @Override
    void setServerSocket(ServerSocket socket) {
        delegate.serverSocket = socket;
        super.setServerSocket(socket);
    }

    @Override
    protected void connect(SocketAddress endpoint, int timeout)
        throws IOException
    {
        if (endpoint == null || !(endpoint instanceof InetSocketAddress))
            throw new IllegalArgumentException("Unsupported address type");
        final InetSocketAddress epoint = (InetSocketAddress)endpoint;
        final String destHost = epoint.isUnresolved() ? epoint.getHostName()
                                                      : epoint.getAddress().getHostAddress();
        final int destPort = epoint.getPort();

        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkConnect(destHost, destPort);

        // Connect to the HTTP proxy server
        String urlString = "http://" + destHost + ":" + destPort;
        Socket httpSocket = privilegedDoTunnel(urlString, timeout);

        // Success!
        external_address = epoint;

        // close the original socket impl and release its descriptor
        close();

        // update the Sockets impl to the impl from the http Socket
        SocketImpl si = httpSocket.impl;
        ((SocketImpl) this).getSocket().setImpl(si);

        // best effort is made to try and reset options previously set
        Set<Map.Entry<Integer,Object>> options = optionsMap.entrySet();
        try {
            for(Map.Entry<Integer,Object> entry : options) {
                si.setOption(entry.getKey(), entry.getValue());
            }
        } catch (IOException x) {  /* gulp! */  }
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        delegate.bind(host, port);
    }

    @Override
    protected void listen(int backlog) throws IOException {
        throw new IllegalStateException();
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        throw new IllegalStateException();
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    @Override
    protected int available() throws IOException {
        return delegate.available();
    }

    @Override
    protected void close() throws IOException {
        delegate.close();
    }

    @Override
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        delegate.setOption(name, value);
    }

    @Override
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @Override
    void reset() throws IOException {
        delegate.reset();
    }

    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

    @Override
    protected boolean supportsUrgentData () {
        return delegate.supportsUrgentData();
    }

    @Override
    public void setOption(int opt, Object val) throws SocketException {
        delegate.setOption(opt, val);

        if (external_address != null)
            return;  // we're connected, just return

        // store options so that they can be re-applied to the impl after connect
        optionsMap.put(opt, val);
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return delegate.getOption(optID);
    }

    private Socket privilegedDoTunnel(final String urlString,
                                      final int timeout)
        throws IOException
    {
        try {
            return java.security.AccessController.doPrivileged(
                new java.security.PrivilegedExceptionAction<>() {
                    public Socket run() throws IOException {
                        return doTunnel(urlString, timeout);
                }
            });
        } catch (java.security.PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
    }

    private Socket doTunnel(String urlString, int connectTimeout)
        throws IOException
    {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(server, port));
        URL destURL = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) destURL.openConnection(proxy);
        conn.setConnectTimeout(connectTimeout);
        int timeout = (int) getOption(SocketOptions.SO_TIMEOUT);
        if (timeout > 0) {
            conn.setReadTimeout(timeout);
        }
        conn.connect();
        doTunneling(conn);
        try {
            Object httpClient = httpField.get(conn);
            return (Socket) serverSocketField.get(httpClient);
        } catch (IllegalAccessException x) {
            throw new InternalError("Should not reach here", x);
        }
    }

    private void doTunneling(HttpURLConnection conn) {
        try {
            doTunneling.invoke(conn);
        } catch (ReflectiveOperationException x) {
            throw new InternalError("Should not reach here", x);
        }
    }

    @Override
    protected InetAddress getInetAddress() {
        if (external_address != null)
            return external_address.getAddress();
        else
            return delegate.getInetAddress();
    }

    @Override
    protected int getPort() {
        if (external_address != null)
            return external_address.getPort();
        else
            return delegate.getPort();
    }


    @Override
    protected FileDescriptor getFileDescriptor() {
        return delegate.getFileDescriptor();
    }

    @Override
    protected void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    @Override
    protected void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    @Override
    protected int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        delegate.sendUrgentData(data);
    }

    @Override
    public SocketImpl delegate() {
        return delegate;
    }

    @Override
    public SocketImpl newInstance() {
        if (delegate instanceof PlainSocketImpl)
            return new HttpConnectSocketImpl(new PlainSocketImpl());
        else if (delegate instanceof NioSocketImpl)
            return new HttpConnectSocketImpl(new NioSocketImpl(false));
        throw new InternalError();
    }

    @Override
    public void postCustomAccept() {
        // TODO
    }
}
