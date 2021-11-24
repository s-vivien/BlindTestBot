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
package fr.svivien.btbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Listener extends ListenerAdapter {
    private final Bot bot;

    public Listener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (event.getJDA().getGuildCache().isEmpty()) {
            Logger log = LoggerFactory.getLogger("MusicBot");
            log.warn("This bot is not on any guilds! Use the following link to add the bot to your guilds!");
            log.warn(event.getJDA().getInviteUrl(BlindTestBot.RECOMMENDED_PERMS));
        }
        credit(event.getJDA());
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        bot.getNowplayingHandler().onMessageDelete(event.getGuild(), event.getMessageIdLong());
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        bot.shutdown();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        credit(event.getJDA());
    }

    // make sure people aren't adding clones to dbots
    private void credit(JDA jda) {
        Guild dbots = jda.getGuildById(110373943822540800L);
        if (dbots == null)
            return;
        if (bot.getConfig().getDBots())
            return;
        jda.getTextChannelById(119222314964353025L)
                .sendMessage("This account is running BlindTestBot. Please do not list bot clones on this server, <@" + bot.getConfig().getOwnerId() + ">.").complete();
        dbots.leave().queue();
    }
}
