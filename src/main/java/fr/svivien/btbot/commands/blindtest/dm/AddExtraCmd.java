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
package fr.svivien.btbot.commands.blindtest.dm;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDMCommand;

import java.util.List;

public class AddExtraCmd extends BTDMCommand {

    public AddExtraCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "addextra";
        this.help = "adds an extra input to guess to the song entry";
        this.arguments = "<song index> <extra name> <extra value>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String author = event.getMessage().getAuthor().getName();
        String[] spl = event.getArgs().split(" ", 3);

        try {
            Integer songIdx = Integer.valueOf(spl[0]);
            String extraName = spl[1];
            String extraValue = spl[2];
            boolean updateResult = blindTest.addExtra(author, songIdx, extraName, extraValue);
            if (!updateResult) event.reply("Error : no submission with that index, invalid extra name, or the maximum number of extras has been reached");
            else event.reply("Extra successfully added");
            List<String> lists = blindTest.getSongList(author);
            for (String list : lists) {
                event.reply(list);
            }
        } catch (Exception e) {
            event.reply("Invalid parameters, expected " + arguments);
        }
    }

}
