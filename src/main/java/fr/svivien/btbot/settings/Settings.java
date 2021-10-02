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
package fr.svivien.btbot.settings;

import com.jagrosh.jdautilities.command.GuildSettingsProvider;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Collection;
import java.util.Collections;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class Settings implements GuildSettingsProvider {
    private final SettingsManager manager;
    protected long textId;
    protected long roleId;

    public Settings(SettingsManager manager, String textId, String roleId) {
        this.manager = manager;
        try {
            this.textId = Long.parseLong(textId);
        } catch (NumberFormatException e) {
            this.textId = 0;
        }
        try {
            this.roleId = Long.parseLong(roleId);
        } catch (NumberFormatException e) {
            this.roleId = 0;
        }
    }

    public Settings(SettingsManager manager, long textId, long roleId) {
        this.manager = manager;
        this.textId = textId;
        this.roleId = roleId;
    }

    // Getters
    public TextChannel getTextChannel(Guild guild) {
        return guild == null ? null : guild.getTextChannelById(textId);
    }

    public Role getRole(Guild guild) {
        return guild == null ? null : guild.getRoleById(roleId);
    }

    @Override
    public Collection<String> getPrefixes() {
        return Collections.EMPTY_SET;
    }

    // Setters
    public void setTextChannel(TextChannel tc) {
        this.textId = tc == null ? 0 : tc.getIdLong();
        this.manager.writeSettings();
    }

    public void setDJRole(Role role) {
        this.roleId = role == null ? 0 : role.getIdLong();
        this.manager.writeSettings();
    }
}
