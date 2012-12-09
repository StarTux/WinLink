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

/**
 * A connection between the local server and a remote host.
 */
public interface ServerConnection {
        /**
         * Get the name that the remote client sent in the
         * handshake.
         * @return the name
         */
        public String getName();

        /**
         * Get the hostname of the connected client.
         * @return the hostname
         */
        public String getRemoteHostname();

        /**
         * Get the port number of the connected client.
         * @return the port number;
         */
        public int getRemotePort();

        /**
         * Send a packet to the remote client.
         * @param packet the packet
         */
        public void sendPacket(Serializable packet);

        /**
         * Get a status message of this connection.
         * @return the status
         */
        public String getStatus();

        public WinLink getWinLink();
}
