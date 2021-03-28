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
package com.jagrosh.jmusicbot.commands.blindtest.pub;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.commands.BTPublicCommand;

public class RulesCmd extends BTPublicCommand {

    private int maximumExtras;
    private String helpWord;

    public RulesCmd(Bot bot, BlindTest blindTest, String helpWord, int maximumExtras) {
        super(bot, blindTest);
        this.helpWord = helpWord;
        this.maximumExtras = maximumExtras;
        this.name = "rules";
        this.help = "prints the rules";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String rules = ":bulb: **Submissions :** :bulb:\n" +
                       "* The song pool is collaborative, everyone can submit tracks to the bot\n" +
                       "* To add a song, send a DM to the bot using the following syntax : `!add <YT url>`. A few seconds later, the bot will respond to you indicating the success/failure of the operation, and the list of songs added so far\n" +
                       "* For every successful submission, the bot will indicate in squares brackets the values the players will have to guess. Make sure they are correct\n" +
                       "* In case they are not, you can update them manually using the commands `!setartist <song index> <artist>` and `!settitle <song index> <title>`\n" +
                       "* Players can also add/remove up to " + maximumExtras + " extra input(s) to guess using the command `!addextra <song index> <extra>` and `!removeextra <song index> <extra index>`\n" +
                       "\n" +
                       ":bulb: **Blind-test rules :** :bulb:\n" +
                       "* When the song starts playing, players type their guesses directly in the chat\n" +
                       "* 1 point is awarded to the first player who finds the correct track title, 1 point for the artist, and 3 points if both of them are simultaneously found (assuming neither of them have been found so far)\n" +
                       "* If extra inputs have been added, 1 point is awarded to the first player who finds each of them (extra inputs are not considered for combos)\n" +
                       "* If none of the inputs (artist, title, extras) of a submission is found, 1 point is removed from the submission's author's score\n" +
                       "\n" +
                       "*Type `!" + helpWord + "`* for the complete list of available commands";
        event.reply(rules);
    }
}
