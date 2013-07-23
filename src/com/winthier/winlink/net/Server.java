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

import org.bukkit.scheduler.BukkitRunnable;
import com.winthier.winlink.ServerConnection;
import com.winthier.winlink.WinLinkPlugin;
import com.winthier.winlink.event.ServerConnectEvent;
import com.winthier.winlink.event.ServerDisconnectEvent;
import com.winthier.winlink.event.ServerReceivePacketEvent;
import com.winthier.winlink.message.MessageRecipient;
import com.winthier.winlink.message.PacketReceivedMessage;
import com.winthier.winlink.message.SendMessageTask;
import com.winthier.winlink.message.SendPacketMessage;
import com.winthier.winlink.message.ServerConnectMessage;
import com.winthier.winlink.message.ServerReconnectMessage;
import com.winthier.winlink.message.ShutdownMessage;
import com.winthier.winlink.message.SocketConnectedMessage;
import com.winthier.winlink.message.SocketDisconnectedMessage;
import com.winthier.winlink.packet.HandshakePacket;
import com.winthier.winlink.packet.KeepAlivePacket;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;

public class Server extends BukkitRunnable implements MessageRecipient {
        private final WinLinkPlugin plugin;
        private String name;
        private ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1024);
        private SocketListener socketListener;
        private int maxConnections = 10;
        private int port = -1;
        private long nextId;
        private Set<ServerClientConnection> handshakePending = new HashSet<ServerClientConnection>();
        private Map<String, ServerClientConnection> connections = Collections.synchronizedMap(new LinkedHashMap<String, ServerClientConnection>());
        private volatile boolean running = true;
        private AtomicReference<String> status = new AtomicReference<String>("N/A");

        public Server(WinLinkPlugin plugin, String name) {
                this.plugin = plugin;
                this.name = name;
        }

        public WinLinkPlugin getPlugin() {
                return plugin;
        }

        synchronized long getNextId() {
                return nextId++;
        }

        @Override
        public void run() {
                try {
                        setStatus("Running");
                        BukkitRunnable task = new BukkitRunnable() {
                                @Override
                                public void run() {
                                        try {
                                                keepAlive();
                                        } catch (Throwable t) {
                                                t.printStackTrace();
                                        }
                                }
                        };
                        task.runTaskTimer(plugin, 5 * 20L, 5 * 20L);
                        while (running) {
                                Object msg = null;
                                try {
                                        msg = queue.take();
                                } catch (InterruptedException ie) {
                                        continue;
                                }
                                if (msg instanceof ShutdownMessage) break;
                                else if (msg instanceof ServerConnectMessage) handleServerConnect((ServerConnectMessage)msg);
                                else if (msg instanceof ServerReconnectMessage) handleServerReconnect((ServerReconnectMessage)msg);
                                else if (msg instanceof SendPacketMessage) broadcast(((SendPacketMessage)msg).packet);
                                else if (msg instanceof SocketConnectedMessage) handleSocketConnected((SocketConnectedMessage)msg);
                                else if (msg instanceof SocketDisconnectedMessage) handleSocketDisconnected((SocketDisconnectedMessage)msg);
                                else if (msg instanceof PacketReceivedMessage) handlePacketReceived((PacketReceivedMessage)msg);
                                else {
                                        plugin.getLogger().warning("Server: Unhandled message type: " + msg.getClass().getName());
                                }
                        }
                        running = false;
                        try { task.cancel(); } catch (IllegalStateException ise) {}
                        if (socketListener != null) socketListener.shutdown();
                        synchronized (connections) {
                                for (ServerClientConnection connection : connections.values()) {
                                        connection.shutdown();
                                }
                        }
                        connections.clear();
                        setStatus("Shut down");
                } catch (Throwable t) {
                        t.printStackTrace();
                }
        }

        public void keepAlive() {
                broadcast(new KeepAlivePacket());
        }

        protected void handleSocketConnected(SocketConnectedMessage msg) {
                ServerClientConnection connection = null;
                try {
                        connection = new ServerClientConnection(msg.socket, this);
                } catch (IOException ioe) {
                        ioe.printStackTrace();
                        return;
                }
                handshakePending.add(connection);
                connection.runTaskAsynchronously(plugin);
                connection.sendPacket(new HandshakePacket(WinLinkPlugin.PROTOCOL_VERSION, name));
                plugin.getLogger().info("Server: connected " + connection.getRemoteHostname() + ":" + connection.getRemotePort());
        }

        protected void handleServerConnect(ServerConnectMessage msg) {
                if (port == msg.port && name != null && name.equals(msg.name)) return;
                port = msg.port;
                name = msg.name;
                if (socketListener != null) {
                        socketListener.shutdown();
                }
                setupListener();
        }

        protected void handleServerReconnect(ServerReconnectMessage msg) {
                if (socketListener != null) return;
                setupListener();
        }

        protected void setupListener() {
                try {
                        socketListener = new SocketListener(this, port);
                } catch (IOException ioe) {
                        setStatus("Failed to bind port: " + ioe.getMessage() + ". Retry within 10 seconds.");
                        // resend msg in 10 seconds
                        reconnect(10);
                        return;
                }
                socketListener.runTaskAsynchronously(plugin);
                setStatus("Listening");
        }

        protected void handleSocketDisconnected(SocketDisconnectedMessage msg) {
                if (!(msg.connection instanceof ServerClientConnection)) return;
                ServerClientConnection connection = (ServerClientConnection)msg.connection;
                if (connections.remove(connection.getName()) != null) {
                        plugin.getLogger().info("Server: disconnected `" + connection.getName() + "' from " + connection.getRemoteHostname() + ":" + connection.getRemotePort() + ": " + msg.cause);
                        plugin.getServer().getPluginManager().callEvent(new ServerDisconnectEvent((ServerConnection)msg.connection, msg.cause));
                        connection.setStatus("Disconnected: " + msg.cause);
                }
        }

        protected void handlePacketReceived(PacketReceivedMessage msg) {
                if (!(msg.source instanceof ServerClientConnection)) return;
                ServerClientConnection connection = (ServerClientConnection)msg.source;
                if (connections.containsValue(connection)) {
                        plugin.getServer().getPluginManager().callEvent(new ServerReceivePacketEvent(connection, msg.packet));
                } else if (handshakePending.contains(connection)) {
                        if (!(msg.packet instanceof HandshakePacket)) {
                                handshakePending.remove(connection);
                                connection.shutdown();
                        }
                        HandshakePacket packet = (HandshakePacket)msg.packet;
                        if (packet.protocolVersion != WinLinkPlugin.PROTOCOL_VERSION) {
                                handshakePending.remove(connection);
                                connection.shutdown();
                        }
                        if (connections.containsKey(packet.name)) {
                                handshakePending.remove(connection);
                                connection.shutdown();
                        }
                        handshakePending.remove(connection);
                        connection.setName(packet.name);
                        connections.put(packet.name, connection);
                        connection.setStatus("Connected");
                        plugin.getLogger().info("Server: accepted `" + connection.getName() + "' from " + connection.getRemoteHostname() + ":" + connection.getRemotePort());
                        plugin.getServer().getPluginManager().callEvent(new ServerConnectEvent(connection));
                        // Connect corresponding client, if any
                        Client client = plugin.getClientConnection(connection.getName());
                        if (client != null) client.reconnect(0);
                } else {
                        return;
                }
        }

        public void shutdown() {
                sendMessage(new ShutdownMessage());
        }

        public void connect(int port, String name) {
                sendMessage(new ServerConnectMessage(port, name));
        }

        public void reconnect(int delaySeconds) {
                if (delaySeconds == 0) {
                        sendMessage(new ServerReconnectMessage());
                } else {
                        new SendMessageTask(this, new ServerReconnectMessage()).runTaskLaterAsynchronously(plugin, delaySeconds * 20);
                }
        }

        /**
         * Broadcast an object to all connected clients.
         */
        protected void broadcast(Serializable packet) {
                synchronized(connections) {
                        for (ServerClientConnection connection : connections.values()) {
                                connection.sendPacket(packet);
                        }
                }
                return;
        }

        public void sendPacket(Serializable packet) {
                sendMessage(new SendPacketMessage(packet));
        }

        @Override
        public boolean sendMessage(Object msg) {
                if (!queue.offer(msg)) {
                        plugin.getLogger().warning("Server: queue is full!");
                        setStatus("Queue overflow!");
                        return false;
                }
                return true;
        }

        public void setMaxConnections(int maxConnections) {
                this.maxConnections = maxConnections;
        }

        public int getPort() {
                return port;
        }

        public String getName() {
                return name;
        }

        /**
         * Get a list with all connections of this Server with
         * remote Clients. This is there so WinLinkPlugin can call
         * it to satisfy WinLink API.
         * @return all connections
         */
        public Collection<ServerClientConnection> getConnections() {
                synchronized(connections) {
                        return new ArrayList<ServerClientConnection>(connections.values());
                }
        }

        public ServerClientConnection getConnection(String name) {
                return connections.get(name);
        }

        public void setStatus(String msg) {
                status.set(msg);
        }

        public String getStatus() {
                return status.get();
        }
}
