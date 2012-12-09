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
import com.winthier.winlink.message.SendPacketMessage;
import com.winthier.winlink.message.ShutdownMessage;
import com.winthier.winlink.message.SocketDisconnectedMessage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import com.winthier.winlink.BukkitRunnable;

public class SocketWriter extends BukkitRunnable implements MessageRecipient {
        protected SocketConnection connection;
        protected ArrayBlockingQueue<Object> input = new ArrayBlockingQueue<Object>(128);
        protected ObjectOutputStream output;

        public SocketWriter(SocketConnection connection) throws IOException {
                this.connection = connection;
        }

        public WinLinkPlugin getPlugin() {
                return WinLinkPlugin.getInstance();
        }

        @Override
        public void run() {
                try {
                        try {
                                output = new ObjectOutputStream(connection.getSocket().getOutputStream());
                        } catch (IOException ioe) {
                                connection.sendMessage(new SocketDisconnectedMessage(connection, DisconnectCause.IO_ERROR));
                                return;
                        }
                        while (true) {
                                Object msg = null;
                                try {
                                        msg = input.take();
                                } catch (InterruptedException ie) {
                                        continue;
                                }
                                if (msg instanceof SendPacketMessage) { // send the packet
                                        SendPacketMessage sendPacket = (SendPacketMessage)msg;
                                        try {
                                                output.writeObject(sendPacket.packet);
                                        } catch (IOException ioe) {
                                                getPlugin().getLogger().warning("SocketWriter.run(): writeObject(): " + ioe);
                                                break;
                                        }
                                } else if (msg instanceof ShutdownMessage) { // shut down
                                        break;
                                } else { // unhandled message type
                                        getPlugin().getLogger().warning("SocketWriter.run(): Unhandled Message type: " + msg.getClass().getName());
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
                try { output.close(); } catch (IOException ioe) {}
                output = null;
                connection.shutdown();
        }

        @Override
        public boolean sendMessage(Object msg) {
                return input.offer(msg);
        }

        public void shutdown() {
                sendMessage(new ShutdownMessage());
        }
}
