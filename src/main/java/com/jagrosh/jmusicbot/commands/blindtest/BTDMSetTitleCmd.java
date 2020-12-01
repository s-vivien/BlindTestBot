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
import com.jagrosh.jmusicbot.BlindTest;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.BTDMCommand;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class BTDMSetTitleCmd extends BTDMCommand {

    private BlindTest blindTest;

    public BTDMSetTitleCmd(Bot bot, BlindTest blindTest) {
        super(bot);
        this.blindTest = blindTest;
        this.name = "settitle";
        this.help = "sets the song title";
        this.arguments = "<ID> <title>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String author = commandEvent.getMessage().getAuthor().getName();
        String[] spl = commandEvent.getArgs().split(" ", 2);

        try {
            Integer idx = Integer.valueOf(spl[0]);
            String title = spl[1];
            int updateResult = blindTest.updateTitle(author, idx, title);
            if (updateResult == 1) commandEvent.reply("Aucune chanson ne correspond à cet index");
            else commandEvent.reply("Titre mis à jour avec succès\n" + blindTest.getSongList(author));
        } catch (Exception e) {
            commandEvent.reply("Paramètres incorrects, les arguments attendus sont " + arguments);
        }
    }

}