/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.commands.BTDJCommand;

public class LockPoolCmd extends BTDJCommand {

    public LockPoolCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "lock";
        this.help = "lock/unlock the submissions";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        boolean lock = blindTest.swapLock();
        event.reply("Submissions are now **" + (lock ? "LOCKED :lock:" : "UNLOCKED :unlock:") + "**");
    }
}
