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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.BlindTest;
import com.jagrosh.jmusicbot.Bot;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class BTDMRemoveCmd extends Command {

    private BlindTest blindTest;
    private Bot bot;

    public BTDMRemoveCmd(Bot bot, BlindTest blindTest) {
        this.blindTest = blindTest;
        this.bot = bot;
        this.name = "remove";
        this.arguments = "<Song index>";
        this.help = "Removes song from the blindtest pool";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String author = commandEvent.getMessage().getAuthor().getName();
        Integer idx;
        try {
            idx = Integer.valueOf(commandEvent.getArgs());
            if (blindTest.removeSongRequest(author, idx) == 0) commandEvent.reply("Chanson retirée avec succès\n" + blindTest.getSongList(author));
            else commandEvent.reply("Aucune chanson ne correspond à cet index");
        } catch (Exception e) {
            commandEvent.reply("Index au mauvais format");
        }
    }

}
