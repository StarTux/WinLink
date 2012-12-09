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

import org.bukkit.Bukkit;
import com.winthier.winlink.BukkitRunnable;

/**
 * This class will send a message to a recipient in a seperate
 * thread. It is there so message sending can be deferred.
 */
public class SendMessageTask extends BukkitRunnable {
        private MessageRecipient recipient;
        private Object msg;

        public SendMessageTask(MessageRecipient recipient, Object msg) {
                this.recipient = recipient;
                this.msg = msg;
        }

        @Override
        public void run() {
                try {
                        recipient.sendMessage(msg);
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public void shutdown() {
                try {
                        cancel();
                } catch (IllegalStateException ise) {
                        ise.printStackTrace();
                }
        }
}
