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

package com.winthier.winlink.event;

import com.winthier.winlink.ClientConnection;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A group of events associated with a connection between a local
 * client and a remote server.
 */
public class ClientEvent extends Event {
        private static HandlerList handlers = new HandlerList();
        protected ClientConnection connection;

        public ClientEvent(ClientConnection connection) {
                super(true);
                this.connection = connection;
        }

        public ClientConnection getConnection() {
                return connection;
        }

        public static HandlerList getHandlerList() {
                return handlers;
        }

        @Override
        public HandlerList getHandlers() {
                return handlers;
        }
}
