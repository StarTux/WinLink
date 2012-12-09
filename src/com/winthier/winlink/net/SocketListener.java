/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Copyright 2012 StarTux
 *
 * This file is part of WinLink.
 *
 * WinLink is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WinLink is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WinLink.  If not, see <http://www.gnu.org/licenses/>.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package com.winthier.winlink.net;

import com.winthier.winlink.WinLinkPlugin;
import com.winthier.winlink.message.SocketConnectedMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import com.winthier.winlink.BukkitRunnable;

/**
 * This class takes care of a ServerSocket and listens to it for
 * connections in its own thread.
 */
public class SocketListener extends BukkitRunnable {
        private Server server;
        private ServerSocket socket;
        private int port;

        public SocketListener(Server server, int port) throws IOException {
                this.server = server;
                this.port = port;
                socket = new ServerSocket(); // throws
                // Try to setReuseAddress, but don't care if it fails
                try {
                        socket.setReuseAddress(true);
                } catch (SocketException se) {}
                socket.bind(new InetSocketAddress(port)); // throws
        }
        
        public WinLinkPlugin getPlugin() {
                return server.getPlugin();
        }

        public void run() {
                while (!socket.isClosed()) {
                        Socket connection = null;
                        try {
                                connection = socket.accept();
                        } catch (Exception e) {
                                // if the socket is closed, we can
                                // assume that it is intentional.
                                if (!socket.isClosed()) {
                                        getPlugin().getLogger().warning("SocketListener: accept() failed: " + e);
                                }
                                break;
                        }
                        server.sendMessage(new SocketConnectedMessage(connection));
                }
                try {
                        socket.close();
                } catch (IOException ioe) {}
        }

        public void shutdown() {
                try {
                        socket.close();
                } catch (IOException ioe) {}
        }

        public int getPort() {
                return port;
        }
}
