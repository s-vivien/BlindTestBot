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
package fr.svivien.btbot.commands.blindtest.pub;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTPublicCommand;

public class RulesCmd extends BTPublicCommand {

    private final int maximumExtras;
    private final String helpWord;
    private final String prefix;

    public RulesCmd(Bot bot, BlindTest blindTest, String helpWord, int maximumExtras, String prefix) {
        super(bot, blindTest);
        this.helpWord = helpWord;
        this.prefix = prefix;
        this.maximumExtras = maximumExtras;
        this.name = "rules";
        this.help = "prints the rules";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String rules = ":bulb: **Submissions :** :bulb:\n" +
                "* The song pool is collaborative, everyone can submit tracks to the bot\n" +
                "* To add a song, send a DM to the bot using the following syntax : `" + prefix + "add <YT url>`. A few seconds later, the bot will respond to you indicating the success/failure of the operation, and the list of songs added so far\n" +
                "* For every successful submission, the bot will indicate in squares brackets the values the players will have to guess. Make sure they are correct\n" +
                "* In case they are not, you can update them manually using the commands `" + prefix + "set <song index> <name> <value>`\n" +
                "* Players can also add/remove up to " + maximumExtras + " extra input(s) to guess using the command `" + prefix + "addextra <song index> <extra name> <extra value>` and `" + prefix + "removeextra <song index> <extra name>`\n" +
                "\n" +
                ":bulb: **Blind-test rules :** :bulb:\n" +
                "* Every player must join the voice channel. The DJ/host will then use the `" + prefix + "next` command to start a random track. The bot will join the voice channel and play the song\n" +
                "* Players type their guesses directly in the chat (the player who submitted the song cannot play)\n" +
                "* The message will be compared to each value to guess (artist, title, extra), **independently**. Don't try to write artist and title in a single message, for instance\n" +
                "* 1 point is awarded each time a player finds a value that wasn't previously guessed. Each subsequent good answer on the same track will reward 2 points\n" +
                "* If none of the inputs (artist, title, extras) of a submission is found, 1 point is removed from the submission's author's score\n" +
                "\n" +
                "The author of the submission can prematurely stop the song by typing the " + prefix + "stop command\n" +
                "If any other player wants to skip the song, they can type `:skip:`. If more than half of the players type that message, the song is stopped" +
                "\n" +
                "*Type `" + prefix + helpWord + "`* for the complete list of available commands";
        event.reply(rules);
    }
}
