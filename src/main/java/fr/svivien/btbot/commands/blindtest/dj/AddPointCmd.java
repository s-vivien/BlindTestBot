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
package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;

public class AddPointCmd extends BTDJCommand {

    public AddPointCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "addpoint";
        this.arguments = "<points> <nick>";
        this.help = "adds points to some user's score";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String[] spl = event.getArgs().split(" ", 2);

        try {
            int pts = Integer.parseInt(spl[0]);
            String nick = spl[1];
            String realNick = blindTest.checkNick(nick);
            if (realNick != null) {
                int score = blindTest.addScore(realNick, pts);
                event.reply(pts + " added" + ((pts > 1 || pts < -1) ? "s" : "") + " to " + realNick + ", who now has " + score);
            } else {
                event.reply("Unknown player..");
            }
        } catch (Exception e) {
            event.reply("Invalid parameters, expected " + arguments);
        }
    }
}
