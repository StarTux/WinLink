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
import com.winthier.winlink.message.SendPacketMessage;
import com.winthier.winlink.message.ShutdownMessage;
import com.winthier.winlink.message.SocketDisconnectedMessage;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import com.winthier.winlink.BukkitRunnable;

public class SocketConnection extends BukkitRunnable implements MessageRecipient {
        protected Socket socket;
        protected SocketReader reader;
        protected SocketWriter writer;
        private ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1024);
        protected MessageRecipient output;

        public SocketConnection(Socket socket, MessageRecipient output) throws IOException {
                this.socket = socket;
                this.output = output;
                socket.setSoTimeout(10000);
                reader = new SocketReader(this);
                writer = new SocketWriter(this);
                reader.runTaskAsynchronously(WinLinkPlugin.getInstance());
                writer.runTaskAsynchronously(WinLinkPlugin.getInstance());
        }

        @Override
        public void run() {
                DisconnectCause disconnectCause = DisconnectCause.OTHER;
                try {
                        while (true) {
                                Object msg = null;
                                try {
                                        msg = queue.take();
                                } catch (InterruptedException ie) {
                                        continue;
                                }
                                if (msg instanceof SendPacketMessage) {
                                        writer.sendMessage(msg);
                                } else if (msg instanceof PacketReceivedMessage) {
                                        output.sendMessage(msg);
                                } else if (msg instanceof ShutdownMessage) {
                                        break;
                                } else if (msg instanceof SocketDisconnectedMessage) {
                                        disconnectCause = ((SocketDisconnectedMessage)msg).cause;
                                        break;
                                } else {
                                        WinLinkPlugin.getInstance().getLogger().warning(getClass().getSimpleName() + " bad message type: " + msg.getClass().getName());
                                }
                        }
                        writer.shutdown();
                        reader.shutdown();
                        try { socket.close(); } catch (IOException ioe) {}
                        output.sendMessage(new SocketDisconnectedMessage(this, disconnectCause));
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public boolean sendMessage(Object msg) {
                return queue.offer(msg);
        }

        public void sendPacket(Serializable packet) {
                sendMessage(new SendPacketMessage(packet));
        }

        public Socket getSocket() {
                return socket;
        }

        public void shutdown() {
                sendMessage(new ShutdownMessage());
        }
}
