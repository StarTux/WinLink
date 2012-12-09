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
 * A connection between a local client and a remote server.
 */
public interface ClientConnection {
        /**
         * Get the name of this client as specified in the plugin
         * configuration file.
         * @return the name
         */
        public String getName();

        /**
         * Get the hostname of the remote server as specified in
         * the plugin configuration file.
         * @return the hostname
         */
        public String getRemoteHostname();

        /**
         * Get the portnumber of the remote server as specified in
         * the plugin configuration file.
         * @return the port number
         */
        public int getRemotePort();

        /**
         * Get a status message of this connection.
         * @return the status
         */
        public String getStatus();

        /**
         * Send a packet to the connected remote server.
         * @param packet the packet
         */
        public void sendPacket(Serializable packet);

        /**
         * Check if this client has established a connection.
         * @return true if it is connected, false if not
         */
        public boolean isConnected();

        public WinLink getWinLink();
}
