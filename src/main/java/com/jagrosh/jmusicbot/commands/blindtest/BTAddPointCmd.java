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
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.commands.BTDJCommand;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class BTAddPointCmd extends BTDJCommand {

    private BlindTest blindTest;

    public BTAddPointCmd(BlindTest blindTest) {
        this.blindTest = blindTest;
        this.name = "addpoint";
        this.arguments = "<nick> <points>";
        this.help = "adds points to some user's score";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] spl = commandEvent.getArgs().split(" ");

        try {
            String nick = spl[0];
            Integer pts = Integer.valueOf(spl[1]);
            if (blindTest.isKnownNick(nick)) {
                int score = blindTest.addScore(nick, pts);
                commandEvent.reply(pts + " ajouté" + ((pts > 1 || pts < -1) ? "s" : "") + " à " + nick + ", qui a désormais " + score);
            } else {
                commandEvent.reply("Joueur inconnu..");
            }
        } catch (Exception e) {
            commandEvent.reply("Paramètres incorrects, les arguments attendus sont " + arguments);
        }
    }
}
