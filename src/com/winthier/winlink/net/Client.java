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

import com.winthier.winlink.BukkitRunnable;
import com.winthier.winlink.ClientConnection;
import com.winthier.winlink.DisconnectCause;
import com.winthier.winlink.WinLink;
import com.winthier.winlink.WinLinkPlugin;
import com.winthier.winlink.event.ClientConnectEvent;
import com.winthier.winlink.event.ClientDisconnectEvent;
import com.winthier.winlink.event.ClientReceivePacketEvent;
import com.winthier.winlink.message.ClientConnectMessage;
import com.winthier.winlink.message.ClientReconnectMessage;
import com.winthier.winlink.message.MessageRecipient;
import com.winthier.winlink.message.PacketReceivedMessage;
import com.winthier.winlink.message.SendMessageTask;
import com.winthier.winlink.message.SendPacketMessage;
import com.winthier.winlink.message.ShutdownMessage;
import com.winthier.winlink.message.SocketDisconnectedMessage;
import com.winthier.winlink.packet.HandshakePacket;
import com.winthier.winlink.packet.KeepAlivePacket;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Client extends BukkitRunnable implements MessageRecipient, ClientConnection {
        private final WinLinkPlugin plugin;
        private final String name;
        private ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(256);
        private SocketConnection connection;
        private String hostname;
        private int port = -1;
        private AtomicReference<String> status = new AtomicReference<String>("N/A");
        private AtomicBoolean shookHand = new AtomicBoolean(false);

        public Client(WinLinkPlugin plugin, String name) {
                this.plugin = plugin;
                this.name = name;
        }

        @Override
        public void run() {
                try {
                        while (true) {
                                Object msg = null;
                                try {
                                        msg = queue.take();
                                } catch (InterruptedException ie) {
                                        continue;
                                }
                                if (msg instanceof ShutdownMessage) break;
                                else if (msg instanceof PacketReceivedMessage) handlePacketReceived((PacketReceivedMessage)msg);
                                else if (msg instanceof ClientConnectMessage) handleClientConnect((ClientConnectMessage)msg);
                                else if (msg instanceof ClientReconnectMessage) handleClientReconnect((ClientReconnectMessage)msg);
                                else if (msg instanceof SocketDisconnectedMessage) handleSocketDisconnected((SocketDisconnectedMessage)msg);
                                else if (msg instanceof SendPacketMessage) handleSendPacket((SendPacketMessage)msg);
                                else {
                                        plugin.getLogger().warning("Client " + name + ": Unexpected message type: " + msg.getClass().getSimpleName());
                                }
                        }
                        setStatus("Shut down");
                        if (shookHand.getAndSet(false)) {
                                plugin.getServer().getPluginManager().callEvent(new ClientDisconnectEvent(this, DisconnectCause.SHUTDOWN));
                        }
                        if (connection != null) {
                                connection.shutdown();
                                connection = null;
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        setStatus("Error: " + e);
                }
        }

        @Override
        public boolean sendMessage(Object msg) {
                return queue.offer(msg);
        }

        protected void handlePacketReceived(PacketReceivedMessage msg) {
                if (msg.source != connection) {
                        return;
                }
                if (msg.packet instanceof KeepAlivePacket) {
                        connection.sendPacket(new KeepAlivePacket(((KeepAlivePacket)msg.packet).hash));
                } else {
                        if (!shookHand.get()) {
                                if (!(msg.packet instanceof HandshakePacket)) {
                                        connection.shutdown();
                                        setStatus("Unexpected packet: " + msg.packet.getClass().getName());
                                        return;
                                }
                                HandshakePacket packet = (HandshakePacket)msg.packet;
                                if (packet.protocolVersion > WinLinkPlugin.PROTOCOL_VERSION) {
                                        connection.shutdown();
                                        setStatus("Client outdated");
                                        return;
                                }
                                if (packet.protocolVersion < WinLinkPlugin.PROTOCOL_VERSION) {
                                        connection.shutdown();
                                        setStatus("Server outdated");
                                        return;
                                }
                                if (!name.equals(packet.name)) {
                                        connection.shutdown();
                                        setStatus("Name mismatch: " + packet.name);
                                        return;
                                }
                                setStatus("Connected");
                                shookHand.set(true);
                                plugin.getServer().getPluginManager().callEvent(new ClientConnectEvent(this));
                        } else {
                                plugin.getServer().getPluginManager().callEvent(new ClientReceivePacketEvent(this, msg.packet));
                        }
                }
        }

        protected void handleClientConnect(ClientConnectMessage msg) {
                if (connection != null) {
                        connection.shutdown();
                        connection = null;
                }
                hostname = msg.hostname;
                port = msg.port;
                setupConnection();
        }

        protected void handleClientReconnect(ClientReconnectMessage msg) {
                if (connection != null) return;
                setupConnection();
        }

        protected void setupConnection() {
                try {
                        Socket socket = new Socket(InetAddress.getByName(hostname), port);
                        connection = new SocketConnection(socket, this);
                        connection.runTaskAsynchronously(plugin);
                } catch (Exception e) {
                        setStatus("Connection failed: " + e.getMessage() + ". Retry within 10 seconds");
                        reconnect(10);
                        return;
                }
                connection.sendPacket(new HandshakePacket(WinLinkPlugin.PROTOCOL_VERSION, plugin.getServerName()));
                setStatus("Connecting");
        }

        protected void handleSocketDisconnected(SocketDisconnectedMessage msg) {
                if (msg.connection != connection) return;
                connection.shutdown();
                connection = null;
                reconnect(10);
                if (shookHand.getAndSet(false)) {
                        setStatus("Disconnected: " + msg.cause);
                        plugin.getServer().getPluginManager().callEvent(new ClientDisconnectEvent(this, msg.cause));
                }
        }

        protected void handleSendPacket(SendPacketMessage msg) {
                if (!shookHand.get()) return;
                connection.sendMessage(msg);
        }

        public void reconnect(int delaySeconds) {
                new SendMessageTask(this, new ClientReconnectMessage()).runTaskLaterAsynchronously(plugin, delaySeconds * 20);
        }

        public void connect(String hostname, int port) {
                sendMessage(new ClientConnectMessage(hostname, port));
        }

        public void shutdown() {
                sendMessage(new ShutdownMessage());
        }

        /**
         * Get the name of this client as specified in the plugin
         * configuration file.
         * Satisfies ClientConnection API.
         * @return the name
         */
        @Override
        public String getName() {
                return name;
        }

        /**
         * Get the hostname of the remote server as specified in
         * the plugin configuration file.
         * Satisfies ClientConnection API.
         * @return the hostname
         */
        @Override
        public String getRemoteHostname() {
                return hostname;
        }

        /**
         * Get the portnumber of the remote server as specified in
         * the plugin configuration file.
         * Satisfies ClientConnection API.
         * @return the port number
         */
        @Override
        public int getRemotePort() {
                return port;
        }

        public void setStatus(String msg) {
                status.set(msg);
        }

        public String getStatus() {
                return status.get();
        }

        /**
         * Send a packet to the connected remote server.
         * Satisfies ClientConnection API.
         * @param packet the packet
         */
        @Override
        public void sendPacket(Serializable packet) {
                sendMessage(new SendPacketMessage(packet));
        }

        /**
         * Check if this client has established a connection.
         * Satisfies ClientConnection API.
         * @return true if it is connected, false if not
         */
        public boolean isConnected() {
                return shookHand.get();
        }

        /**
         * Satisfies ClientConnection API.
         * @return WinLink
         */
        public WinLink getWinLink() {
                return WinLinkPlugin.getInstance();
        }
}
