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

import com.winthier.winlink.net.Client;
import com.winthier.winlink.net.Server;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class WinLinkPlugin extends JavaPlugin implements WinLink {
        private static WinLinkPlugin instance;
        private Server server;
        private Map<String, Client> clients = Collections.synchronizedMap(new LinkedHashMap<String, Client>());
        public static final long PROTOCOL_VERSION = 1;

        @Override
        public void onEnable() {
                instance = this;
                getLogger(); // make sure the logger has been created
                getConfig().options().copyDefaults(true);
                saveConfig();
                loadConfiguration();
        }

        @Override
        public void onDisable() {
                server.shutdown();
                synchronized(clients) {
                        for (Client client : clients.values()) client.shutdown();
                }
                clients.clear();
                instance = null;
        }

        public static WinLinkPlugin getInstance() {
                return instance;
        }

        public static WinLink getWinLink() {
                return instance;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String token, String[] args) {
                if (args.length == 1 && args[0].equals("status")) {
                        sender.sendMessage("[WinLink] status report");
                        sender.sendMessage(String.format("Server (%s, %d): %s", server.getName(), server.getPort(), server.getStatus()));
                        for (ServerConnection connection : server.getConnections()) {
                                sender.sendMessage(String.format("- %s (%s:%d): %s", connection.getName(), connection.getRemoteHostname(), connection.getRemotePort(), connection.getStatus()));
                        }
                        sender.sendMessage("Clients:");
                        for (ClientConnection connection : getClientConnections()) {
                                sender.sendMessage(String.format("- %s (%s:%d): %s", connection.getName(), connection.getRemoteHostname(), connection.getRemotePort(), connection.getStatus()));
                        }
                } else if (args.length == 1 && args[0].equals("reload")) {
                        try {
                                reloadConfig();
                                loadConfiguration();
                                sender.sendMessage("Configuration reloaded");
                        } catch (Exception e) {
                                e.printStackTrace();
                                sender.sendMessage("An error occured while reloading the configuration. See console.");
                        }
                } else if (args.length == 1 && args[0].equals("reconnect")) {
                        sender.sendMessage("Attempting to reconnect all client connections");
                        for (Client client : getClientConnections()) client.reconnect(0);
                } else {
                        sender.sendMessage("Usage: /winlink [subcommand] ...");
                        sender.sendMessage("Subcommands: status, reload");
                }
                return true;
        }

        public void loadConfiguration() {
                // send server information
                ConfigurationSection serverSection = getConfig().getConfigurationSection("server");
                if (serverSection == null) {
                        serverSection = getConfig().createSection("server");
                }
                String serverName = serverSection.getString("Name", "noname");
                String configPath = getConfig().getString("ConfigFile", null);
                ConfigurationSection clientsSection;
                int serverPort;
                Set<String> newClients;
                if (configPath != null && configPath.length() > 0) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(configPath));
                        clientsSection = config.getConfigurationSection("nodes");
                        if (clientsSection == null) clientsSection = getConfig().createSection("clients");
                        serverPort = clientsSection.getInt(serverName + ".Port", 1337);
                        newClients = clientsSection.getKeys(false);
                        newClients.remove(serverName);
                } else {
                        clientsSection = getConfig().getConfigurationSection("clients");
                        if (clientsSection == null) clientsSection = getConfig().createSection("clients");
                        serverPort = serverSection.getInt("Port", 1337);
                        newClients = clientsSection.getKeys(false);
                }
                if (server == null) {
                        server = new Server(this, serverName);
                        server.runTaskAsynchronously(this);
                }
                server.connect(serverPort, serverName);
                // remove obsolete clients
                synchronized (clients) {
                        for (String clientName : clients.keySet()) {
                                if (!newClients.contains(clientName)) {
                                        clients.get(clientName).shutdown();
                                        clients.remove(clientName);
                                }
                        }
                }
                // add new clients
                for (String clientName : newClients) {
                        Client client = clients.get(clientName);
                        if (client == null) {
                                client = new Client(this, clientName);
                                clients.put(clientName, client);
                                client.runTaskAsynchronously(this);
                        }
                        // send client information
                        ConfigurationSection clientSection = clientsSection.getConfigurationSection(clientName);
                        String hostname = clientSection.getString("Hostname", "localhost");
                        int port = clientSection.getInt("Port", 1338);
                        client.connect(hostname, port);
                }
        }

        public void reloadConfiguration() {
                reloadConfig();
                loadConfiguration();
        }

        @Override
        public Collection<? extends ServerConnection> getServerConnections() {
                return server.getConnections();
        }

        @Override
        public ServerConnection getServerConnection(String name) {
                return server.getConnection(name);
        }

        @Override
        public Collection<? extends Client> getClientConnections() {
                synchronized(clients) {
                        return new ArrayList<Client>(clients.values());
                }
        }

        @Override
        public Client getClientConnection(String name) {
                return clients.get(name);
        }

        @Override
        public void broadcastPacket(Serializable packet) {
                server.sendPacket(packet);
        }

        @Override
        public String getServerName() {
                return server.getName();
        }
}
