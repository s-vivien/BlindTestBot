/*
 * Copyright 2018 John Grosh (jagrosh)
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

import fr.svivien.btbot.entities.Prompt;
import fr.svivien.btbot.utils.FormatUtil;
import fr.svivien.btbot.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author John Grosh (jagrosh)
 */
public class BotConfig {
    private final Prompt prompt;
    private final static String CONTEXT = "Config";
    private final static String START_TOKEN = "/// START OF JMUSICBOT CONFIG ///";
    private final static String END_TOKEN = "/// END OF JMUSICBOT CONFIG ///";

    private Path path = null;
    private String token, prefix, altprefix, helpWord, successEmoji, warningEmoji, errorEmoji, loadingEmoji, backupPath;
    private boolean stayInChannel, songInGame, npImages, dbots;
    private long owner, maxSeconds;
    private int songsPerPlayer, maximumExtrasNumber;
    private OnlineStatus status;
    private Activity game;
    private Config aliases;

    private boolean valid = false;

    public BotConfig(Prompt prompt) {
        this.prompt = prompt;
    }

    public void load() {
        valid = false;

        // read config from file
        try {
            // get the path to the config, default config.txt
            path = OtherUtil.getPath(System.getProperty("config.txt", System.getProperty("config", "config.txt")));
            if (path.toFile().exists()) {
                if (System.getProperty("config.txt") == null)
                    System.setProperty("config.txt", System.getProperty("config", "config.txt"));
                ConfigFactory.invalidateCaches();
            }

            // load in the config file, plus the default values
            Config config = ConfigFactory.parseFile(path.toFile()).withFallback(ConfigFactory.load());
            //            Config config = ConfigFactory.load();

            // set values
            backupPath = config.getString("backupPath");
            token = config.getString("token");
            prefix = config.getString("prefix");
            altprefix = config.getString("altprefix");
            helpWord = config.getString("help");
            owner = config.getLong("owner");
            successEmoji = config.getString("success");
            warningEmoji = config.getString("warning");
            errorEmoji = config.getString("error");
            loadingEmoji = config.getString("loading");
            game = OtherUtil.parseGame(config.getString("game"));
            status = OtherUtil.parseStatus(config.getString("status"));
            stayInChannel = config.getBoolean("stayinchannel");
            songInGame = config.getBoolean("songinstatus");
            npImages = config.getBoolean("npimages");
            maxSeconds = config.getLong("maxtime");
            aliases = config.getConfig("aliases");
            dbots = owner == 113156185389092864L;
            songsPerPlayer = config.getInt("songsPerPlayer");
            maximumExtrasNumber = config.getInt("maximumExtrasNumber");

            // we may need to write a new config file
            boolean write = false;

            // validate bot token
            if (token == null || token.isEmpty() || token.equalsIgnoreCase("BOT_TOKEN_HERE")) {
                token = prompt.prompt("Please provide a bot token."
                                      + "\nInstructions for obtaining a token can be found here:"
                                      + "\nhttps://github.com/jagrosh/MusicBot/wiki/Getting-a-Bot-Token."
                                      + "\nBot Token: ");
                if (token == null) {
                    prompt.alert(Prompt.Level.WARNING, CONTEXT, "No token provided! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                    return;
                } else {
                    write = true;
                }
            }

            // validate bot owner
            if (owner <= 0) {
                try {
                    owner = Long.parseLong(prompt.prompt("Owner ID was missing, or the provided owner ID is not valid."
                                                         + "\nPlease provide the User ID of the bot's owner."
                                                         + "\nInstructions for obtaining your User ID can be found here:"
                                                         + "\nhttps://github.com/jagrosh/MusicBot/wiki/Finding-Your-User-ID"
                                                         + "\nOwner User ID: "));
                } catch (NumberFormatException | NullPointerException ex) {
                    owner = 0;
                }
                if (owner <= 0) {
                    prompt.alert(Prompt.Level.ERROR, CONTEXT, "Invalid User ID! Exiting.\n\nConfig Location: " + path.toAbsolutePath().toString());
                    return;
                } else {
                    write = true;
                }
            }

            if (write)
                writeToFile();

            // if we get through the whole config, it's good to go
            valid = true;
        } catch (ConfigException ex) {
            prompt.alert(Prompt.Level.ERROR, CONTEXT, ex + ": " + ex.getMessage() + "\n\nConfig Location: " + path.toAbsolutePath().toString());
        }
    }

    private void writeToFile() {
        String original = OtherUtil.loadResource(this, "/reference.conf");
        byte[] bytes;
        if (original == null) {
            bytes = ("token = " + token + "\r\nowner = " + owner).getBytes();
        } else {
            bytes = original.substring(original.indexOf(START_TOKEN) + START_TOKEN.length(), original.indexOf(END_TOKEN))
                    .replace("BOT_TOKEN_HERE", token)
                    .replace("0 // OWNER ID", Long.toString(owner))
                    .trim().getBytes();
        }
        try {
            Files.write(path, bytes);
        } catch (IOException ex) {
            prompt.alert(Prompt.Level.WARNING, CONTEXT, "Failed to write new config options to config.txt: " + ex
                                                        + "\nPlease make sure that the files are not on your desktop or some other restricted area.\n\nConfig Location: "
                                                        + path.toAbsolutePath().toString());
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getConfigLocation() {
        return path.toFile().getAbsolutePath();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getAltPrefix() {
        return "NONE".equalsIgnoreCase(altprefix) ? null : altprefix;
    }

    public String getToken() {
        return token;
    }

    public long getOwnerId() {
        return owner;
    }

    public String getSuccess() {
        return successEmoji;
    }

    public String getWarning() {
        return warningEmoji;
    }

    public String getError() {
        return errorEmoji;
    }

    public String getLoading() {
        return loadingEmoji;
    }

    public Activity getGame() {
        return game;
    }

    public OnlineStatus getStatus() {
        return status;
    }

    public String getHelp() {
        return helpWord;
    }

    public boolean getStay() {
        return stayInChannel;
    }

    public boolean getSongInStatus() {
        return songInGame;
    }

    public boolean getDBots() {
        return dbots;
    }

    public boolean useNPImages() {
        return npImages;
    }

    public long getMaxSeconds() {
        return maxSeconds;
    }

    public String getMaxTime() {
        return FormatUtil.formatTime(maxSeconds * 1000);
    }

    public int getSongsPerPlayer() {
        return songsPerPlayer;
    }

    public int getMaximumExtrasNumber() {
        return maximumExtrasNumber;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public boolean isTooLong(AudioTrack track) {
        if (maxSeconds <= 0)
            return false;
        return Math.round(track.getDuration() / 1000.0) > maxSeconds;
    }

    public String[] getAliases(String command) {
        try {
            return aliases.getStringList(command).toArray(new String[0]);
        } catch (NullPointerException | ConfigException.Missing e) {
            return new String[0];
        }
    }
}
