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
package com.jagrosh.jmusicbot.commands.blindtest;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.commands.BTDMCommand;

import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class BTDMSetArtistCmd extends BTDMCommand {

    private BlindTest blindTest;

    public BTDMSetArtistCmd(Bot bot, BlindTest blindTest) {
        super(bot);
        this.blindTest = blindTest;
        this.name = "setartist";
        this.help = "sets the song artist";
        this.arguments = "<song index> <artist>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String author = commandEvent.getMessage().getAuthor().getName();
        String[] spl = commandEvent.getArgs().split(" ", 2);

        try {
            Integer idx = Integer.valueOf(spl[0]);
            String artist = spl[1];
            boolean updateResult = blindTest.updateArtist(author, idx, artist);
            if (!updateResult) commandEvent.reply("Error : could not find any submission with that index");
            else commandEvent.reply("Song artist successfully updated");
            List<String> lists = blindTest.getSongList(author);
            for (String list : lists) {
                commandEvent.reply(list);
            }
        } catch (Exception e) {
            commandEvent.reply("Invalid parameters, expected " + arguments);
        }
    }

}
