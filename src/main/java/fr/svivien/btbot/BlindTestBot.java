/*
 * Copyright 2016 John Grosh (jagrosh).
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

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.blindtest.PropositionListener;
import fr.svivien.btbot.commands.blindtest.dj.*;
import fr.svivien.btbot.commands.blindtest.dm.*;
import fr.svivien.btbot.commands.blindtest.pub.PlaylistCmd;
import fr.svivien.btbot.commands.blindtest.pub.PoolCmd;
import fr.svivien.btbot.commands.blindtest.pub.RulesCmd;
import fr.svivien.btbot.commands.blindtest.pub.ScoreboardCmd;
import fr.svivien.btbot.commands.owner.*;
import fr.svivien.btbot.entities.Prompt;
import fr.svivien.btbot.gui.GUI;
import fr.svivien.btbot.settings.Settings;
import fr.svivien.btbot.settings.SettingsManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author John Grosh (jagrosh)
 */
public class BlindTestBot {
    public final static String PLAY_EMOJI = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI = "\u23F9"; // ⏹
    public final static Permission[] RECOMMENDED_PERMS = {Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
            Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE};
    public final static GatewayIntent[] INTENTS = {GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES};

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // startup log
        Logger log = LoggerFactory.getLogger("Startup");

        // create prompt to handle startup
        Prompt prompt = new Prompt("BlindTestBot", "Switching to nogui mode. You can manually start in nogui mode by including the -Dnogui=true flag.",
                "true".equalsIgnoreCase(System.getProperty("nogui", "false")));

        // check for valid java version
        if (!System.getProperty("java.vm.name").contains("64"))
            prompt.alert(Prompt.Level.WARNING, "Java Version", "It appears that you may not be using a supported Java version. Please use 64-bit java.");

        // load config
        BotConfig config = new BotConfig(prompt);
        config.load();
        if (!config.isValid())
            return;

        // set up the listener
        EventWaiter waiter = new EventWaiter();
        SettingsManager settings = new SettingsManager();
        Bot bot = new Bot(waiter, config, settings);

        // set up the blind-test instance
        BlindTest blindTest = new BlindTest(config);
        PropositionListener propositionListener = new PropositionListener(blindTest);

        // set up the command client
        CommandClientBuilder cb = new CommandClientBuilder()
                .setPrefix(config.getPrefix())
                .setListener(propositionListener)
                .setAlternativePrefix(config.getAltPrefix())
                .setOwnerId(Long.toString(config.getOwnerId()))
                .setEmojis(config.getSuccess(), config.getWarning(), config.getError())
                .setHelpWord(config.getHelp())
                .setLinkedCacheSize(200)
                .setGuildSettingsManager(settings)
                .addCommands(
                        new NextCmd(bot, blindTest),
                        new StopCmd(bot, blindTest),
                        new PlayCmd(bot, blindTest),
                        new PauseCmd(bot, blindTest),
                        new AddPointCmd(bot, blindTest),
                        new LimitCmd(bot, blindTest),
                        new LockPoolCmd(bot, blindTest),
                        new ResetCmd(bot, blindTest),
                        new RestoreCmd(bot, blindTest),
                        new BackupCmd(bot, blindTest),
                        new AddCmd(bot, blindTest),
                        new RemoveCmd(bot, blindTest),
                        new ListCmd(bot, blindTest),
                        new SetCmd(bot, blindTest),
                        new UnsetCmd(bot, blindTest),
                        new TimestampCmd(bot, blindTest),
                        new PoolCmd(bot, blindTest),
                        new ScoreboardCmd(bot, blindTest),
                        new RulesCmd(bot, blindTest, config.getHelp(), config.getMaximumExtrasNumber(), config.getPrefix()),
                        new PlaylistCmd(bot, blindTest),
                        new SetdjCmd(bot),
                        new SettcCmd(bot, blindTest),
                        new DebugCmd(bot),
                        new SetavatarCmd(bot),
                        new SetnameCmd(bot),
                        new ShutdownCmd(bot)
                );

        boolean nogame = false;
        if (config.getStatus() != OnlineStatus.UNKNOWN)
            cb.setStatus(config.getStatus());
        if (config.getGame() == null)
            cb.useDefaultGame();
        else if (config.getGame().getName().equalsIgnoreCase("none")) {
            cb.setActivity(null);
            nogame = true;
        } else
            cb.setActivity(config.getGame());

        if (!prompt.isNoGUI()) {
            try {
                GUI gui = new GUI(bot);
                bot.setGUI(gui);
                gui.init();
            } catch (Exception e) {
                log.error("Could not start GUI. If you are "
                          + "running on a server or in a location where you cannot display a "
                          + "window, please run in nogui mode using the -Dnogui=true flag.");
            }
        }

        log.info("Loaded config from " + config.getConfigLocation());

        // attempt to log in and start
        try {
            CommandClient cmdClient = cb.build();
            JDA jda = JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE)
                    .setActivity(nogame ? null : Activity.playing("loading..."))
                    .setStatus(config.getStatus() == OnlineStatus.INVISIBLE || config.getStatus() == OnlineStatus.OFFLINE
                            ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB)
                    .addEventListeners(cmdClient, waiter, new Listener(bot))
                    .setBulkDeleteSplittingEnabled(true)
                    .build();
            bot.setJDA(jda);

            // check single-server
            jda.awaitStatus(JDA.Status.CONNECTED);
            List<Guild> guilds = jda.getGuilds();
            if (guilds.size() != 1) {
                prompt.alert(Prompt.Level.ERROR, "BlindTestBot", "The bot can not be installed on more than 1 server. It is currently on servers : " + guilds.stream().map(Guild::getName).collect(Collectors.joining(", ")));
                System.exit(1);
            }
            // set up blind-test channel
            Guild guild = guilds.get(0);
            Settings s = cmdClient.getSettingsFor(guild);
            TextChannel textChannel = s.getTextChannel(guild);
            if (textChannel == null) {
                List<TextChannel> channels = guild.getTextChannels();
                textChannel = channels.get(0);
                s.setTextChannel(textChannel);
                textChannel.sendMessage("Blind-Test commands can now only be used in <#" + textChannel.getId() + ">").queue();
            }
            blindTest.setBtChannel(textChannel);
        } catch (LoginException ex) {
            prompt.alert(Prompt.Level.ERROR, "BlindTestBot", ex + "\nPlease make sure you are "
                                                          + "editing the correct config.txt file, and that you have used the "
                                                          + "correct token (not the 'secret'!)\nConfig Location: " + config.getConfigLocation());
            System.exit(1);
        } catch (IllegalArgumentException ex) {
            prompt.alert(Prompt.Level.ERROR, "BlindTestBot", "Some aspect of the configuration is "
                                                          + "invalid: " + ex + "\nConfig Location: " + config.getConfigLocation());
            System.exit(1);
        } catch (InterruptedException e) {
            prompt.alert(Prompt.Level.ERROR, "BlindTestBot", "Error while awaiting for JDA to connect");
            System.exit(1);
        }
        blindTest.restoreState("AUTO");
    }
}
