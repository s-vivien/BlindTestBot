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
package com.jagrosh.jmusicbot.commands.blindtest.dm;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.commands.BTDMCommand;

import java.util.List;

public class SetEntryStart extends BTDMCommand {

    public SetEntryStart(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "setstart";
        this.help = "sets the time at which a song should be started, in seconds";
        this.arguments = "<song index> <time>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent commandEvent) {
        String author = commandEvent.getMessage().getAuthor().getName();
        String[] spl = commandEvent.getArgs().split(" ", 2);

        try {
            Integer songIdx = Integer.valueOf(spl[0]);
            Integer offset = Integer.valueOf(spl[1]);
            boolean updateResult = blindTest.setOffset(author, songIdx, offset);
            if (!updateResult) commandEvent.reply("Error : could not find any submission with that index");
            else commandEvent.reply("Start time successfully updated");
            List<String> lists = blindTest.getSongList(author);
            for (String list : lists) {
                commandEvent.reply(list);
            }
        } catch (Exception e) {
            commandEvent.reply("Invalid parameters, expected " + arguments);
        }
    }

}