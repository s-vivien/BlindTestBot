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
package com.jagrosh.jmusicbot.audio;

import com.jagrosh.jmusicbot.JMusicBot;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.nio.ByteBuffer;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {
    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;
    private AudioFrame lastFrame;
    private Runnable onTrackEndLambda;

    public void setOnTrackEndLambda(Runnable onTrackEndLambda) {
        this.onTrackEndLambda = onTrackEndLambda;
    }

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player) {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();
    }

    public void addTrack(QueuedTrack qtrack) {
        if (audioPlayer.getPlayingTrack() == null) {
            audioPlayer.playTrack(qtrack.getTrack());
        } else
            throw new RuntimeException("Should not happen");
    }

    public void stopAndClear() {
        audioPlayer.stopTrack();
    }

    public boolean isMusicPlaying(JDA jda) {
        return guild(jda).getSelfMember().getVoiceState().inVoiceChannel() && audioPlayer.getPlayingTrack() != null;
    }

    public AudioPlayer getPlayer() {
        return audioPlayer;
    }

    public long getRequester() {
        if (audioPlayer.getPlayingTrack() == null || audioPlayer.getPlayingTrack().getUserData(Long.class) == null)
            return 0;
        return audioPlayer.getPlayingTrack().getUserData(Long.class);
    }

    // Audio Events
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (onTrackEndLambda != null) onTrackEndLambda.run();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, track, this);
    }

    // Formatting
    public Message getNowPlaying(JDA jda) {
        if (isMusicPlaying(jda)) {
            Guild guild = guild(jda);
            AudioTrack track = audioPlayer.getPlayingTrack();
            MessageBuilder mb = new MessageBuilder();
            mb.append(FormatUtil.filter(manager.getBot().getConfig().getSuccess() + " **Now Playing in " + guild.getSelfMember().getVoiceState().getChannel().getName() + "...**"));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(guild.getSelfMember().getColor());
            if (getRequester() != 0) {
                User u = guild.getJDA().getUserById(getRequester());
                if (u == null)
                    eb.setAuthor("Unknown (ID:" + getRequester() + ")", null, null);
                else
                    eb.setAuthor(u.getName() + "#" + u.getDiscriminator(), null, u.getEffectiveAvatarUrl());
            }

            try {
                eb.setTitle(track.getInfo().title, track.getInfo().uri);
            } catch (Exception e) {
                eb.setTitle(track.getInfo().title);
            }

            if (track instanceof YoutubeAudioTrack && manager.getBot().getConfig().useNPImages()) {
                eb.setThumbnail("https://img.youtube.com/vi/" + track.getIdentifier() + "/mqdefault.jpg");
            }

            if (track.getInfo().author != null && !track.getInfo().author.isEmpty())
                eb.setFooter("Source: " + track.getInfo().author, null);

            double progress = (double) audioPlayer.getPlayingTrack().getPosition() / track.getDuration();
            eb.setDescription((audioPlayer.isPaused() ? JMusicBot.PAUSE_EMOJI : JMusicBot.PLAY_EMOJI)
                              + " " + FormatUtil.progressBar(progress)
                              + " `[" + FormatUtil.formatTime(track.getPosition()) + "/" + FormatUtil.formatTime(track.getDuration()) + "]` "
                              + FormatUtil.volumeIcon(audioPlayer.getVolume()));

            return mb.setEmbed(eb.build()).build();
        } else return null;
    }

    public Message getNoMusicPlaying(JDA jda) { // TODO
        Guild guild = guild(jda);
        return new MessageBuilder()
                .setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess() + " **Now Playing...**"))
                .setEmbed(new EmbedBuilder()
                        .setTitle("No music playing")
                        .setDescription(JMusicBot.STOP_EMOJI + " " + FormatUtil.progressBar(-1) + " " + FormatUtil.volumeIcon(audioPlayer.getVolume()))
                        .setColor(guild.getSelfMember().getColor())
                        .build()).build();
    }

    public String getTopicFormat(JDA jda) { // TODO
        if (isMusicPlaying(jda)) {
            long userid = getRequester();
            AudioTrack track = audioPlayer.getPlayingTrack();
            String title = track.getInfo().title;
            if (title == null || title.equals("Unknown Title"))
                title = track.getInfo().uri;
            return "**" + title + "** [" + (userid == 0 ? "autoplay" : "<@" + userid + ">") + "]"
                   + "\n" + (audioPlayer.isPaused() ? JMusicBot.PAUSE_EMOJI : JMusicBot.PLAY_EMOJI) + " "
                   + "[" + FormatUtil.formatTime(track.getDuration()) + "] "
                   + FormatUtil.volumeIcon(audioPlayer.getVolume());
        } else return "No music playing " + JMusicBot.STOP_EMOJI + " " + FormatUtil.volumeIcon(audioPlayer.getVolume());
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    // Private methods
    private Guild guild(JDA jda) {
        return jda.getGuildById(guildId);
    }
}
