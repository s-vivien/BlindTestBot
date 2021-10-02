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

public class PlaylistCmd extends BTPublicCommand {

    public PlaylistCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "playlist";
        this.help = "generates a YT playlist of all the songs that have been played so far";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        blindTest.printPoolPlaylists();
    }
}
