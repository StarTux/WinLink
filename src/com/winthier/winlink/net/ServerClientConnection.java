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
import com.winthier.winlink.ServerConnection;
import com.winthier.winlink.WinLink;
import com.winthier.winlink.message.MessageRecipient;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class ServerClientConnection extends SocketConnection implements ServerConnection {
        private String name;
        private AtomicReference<String> status = new AtomicReference<String>("N/A");

        public ServerClientConnection(Socket socket, MessageRecipient output) throws IOException {
                super(socket, output);
        }

        /**
         * Get the name of the remote client.
         * Satisfies ServerConnection API.
         * @return the name
         */
        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        /**
         * Get the hostname of the connected client.
         * Satisfies ServerConnection API.
         * @return the hostname
         */
        @Override
        public String getRemoteHostname() {
                return socket.getInetAddress().getHostName();
        }

        /**
         * Get the port number of the connected client.
         * Satisfies ServerConnection API.
         * @return the port number;
         */
        @Override
        public int getRemotePort() {
                return socket.getPort();
        }

        /**
         * Send a packet to the remote client.
         * Satisfies ServerConnection API.
         * @param packet the packet
         */
        @Override
        public void sendPacket(Serializable packet) {
                super.sendPacket(packet);
        }

        /**
         * Satisfies ServerConnection API.
         * @return WinLink
         */
        @Override
        public WinLink getWinLink() {
                return WinLinkPlugin.getInstance();
        }

        public void setStatus(String status) {
                this.status.set(status);
        }

        public String getStatus() {
                return status.get();
        }
}
