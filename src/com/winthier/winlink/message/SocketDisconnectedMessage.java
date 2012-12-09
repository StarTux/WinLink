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

package com.winthier.winlink.message;

import java.net.Socket;

import com.winthier.winlink.net.SocketConnection;
import com.winthier.winlink.DisconnectCause;

/**
 * Sent from SocketReader to SocketConnection and then to Client
 * or Server when the connection was terminated.
 */
public class SocketDisconnectedMessage {
        public final SocketConnection connection;
        public final DisconnectCause cause;

        public SocketDisconnectedMessage(SocketConnection connection, DisconnectCause cause) {
                this.connection = connection;
                this.cause = cause;
        }

        public SocketDisconnectedMessage(SocketConnection connection) {
                this(connection, DisconnectCause.OTHER);
        }
}
