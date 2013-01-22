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
import com.winthier.winlink.DisconnectCause;
import com.winthier.winlink.message.MessageRecipient;
import com.winthier.winlink.message.PacketReceivedMessage;
import com.winthier.winlink.message.SocketDisconnectedMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketTimeoutException;
import com.winthier.winlink.BukkitRunnable;

/**
 * The SocketReader will read packets from a Socket and send them
 * via messaging back to the connection it belongs to.
 */
public class SocketReader extends BukkitRunnable {
        protected SocketConnection connection; // also output
        protected ObjectInputStream input;

        public SocketReader(SocketConnection connection) throws IOException {
                this.connection = connection;
        }
        
        public WinLinkPlugin getPlugin() {
                return WinLinkPlugin.getInstance();
        }

        @Override
        public void run() {
                try {
                        try {
                                input = new ObjectInputStream(connection.getSocket().getInputStream());
                        } catch (IOException ioe) {
                                connection.sendMessage(new SocketDisconnectedMessage(connection, DisconnectCause.IO_ERROR));
                                return;
                        }
                        DisconnectCause disconnectCause = DisconnectCause.OTHER;
                        while (true) {
                                Object o = null;
                                try {
                                        o = input.readObject();
                                } catch (ClassNotFoundException cnfe) {
                                        continue;
                                } catch (SocketTimeoutException ste) {
                                        disconnectCause = DisconnectCause.READ_TIMEOUT;
                                        break;
                                } catch (IOException ioe) {
                                        disconnectCause = DisconnectCause.IO_ERROR;
                                        break;
                                }
                                connection.sendMessage(new PacketReceivedMessage(o, connection));
                        }
                        try { input.close(); } catch (IOException ioe) {}
                        input = null;
                        connection.sendMessage(new SocketDisconnectedMessage(connection, disconnectCause));
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public void shutdown() {
                try {
                        input.close();
                } catch (Exception e) {}
        }
}
