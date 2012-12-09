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

package com.winthier.winlink;

import java.io.Serializable;
import java.util.Collection;

/**
 * The root of the API.
 */
public interface WinLink {
        /**
         * Get a list of all connections of the local server to
         * remote clients.
         * @return the list
         */
        public Collection<? extends ServerConnection> getServerConnections();

        /**
         * Get one specific connection between the local server
         * and a remote client by name of the remote client.
         * @param name the name
         * @return the connection or null if one by that name does
         * not exist
         */
        public ServerConnection getServerConnection(String name);

        /**
         * Get a list of all connections between local clients and
         * remote servers.
         * @return the list
         */
        public Collection<? extends ClientConnection> getClientConnections();

        /**
         * Get one specific connection between a local client and
         * a remote server by the name of the client as specified
         * in the configuration file.
         * @param name the name
         * @return the connection or null if one by that name does
         * not exist
         */
        public ClientConnection getClientConnection(String name);

        /**
         * Send a packet to all remote clients connected to this
         * server.
         * @param packet The packet
         */
        public void broadcastPacket(Serializable packet);

        /**
         * Get the name of the local server as specified in the configuration file.
         * @return the name
         */
        public String getServerName();
}
