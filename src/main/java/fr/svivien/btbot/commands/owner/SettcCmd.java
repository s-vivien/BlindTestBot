/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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
package fr.svivien.btbot.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.OwnerCommand;
import fr.svivien.btbot.settings.Settings;
import fr.svivien.btbot.utils.FormatUtil;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SettcCmd extends OwnerCommand {

    private BlindTest blindTest;

    public SettcCmd(Bot bot, BlindTest blindTest) {
        this.blindTest = blindTest;
        this.name = "settc";
        this.help = "sets the text channel for blind-test commands";
        this.arguments = "<channel>";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + " Please include a text channel");
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        List<TextChannel> list = FinderUtil.findTextChannels(event.getArgs(), event.getGuild());
        if (list.isEmpty())
            event.reply(event.getClient().getWarning() + " No Text Channels found matching \"" + event.getArgs() + "\"");
        else if (list.size() > 1)
            event.reply(event.getClient().getWarning() + FormatUtil.listOfTChannels(list, event.getArgs()));
        else {
            s.setTextChannel(list.get(0));
            blindTest.setBtChannel(list.get(0));
            event.reply(event.getClient().getSuccess() + " Blind-Test commands can now only be used in <#" + list.get(0).getId() + ">");
        }
    }

}
