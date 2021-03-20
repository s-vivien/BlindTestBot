package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.impl.AnnotatedModuleCompilerImpl;
import com.jagrosh.jdautilities.command.impl.CommandClientImpl;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;

public class SingleChannelCommandClientImpl extends CommandClientImpl {

    private String blindTestChannel;

    public SingleChannelCommandClientImpl(CommandClient client, String blindTestChannel) {
        super(client.getOwnerId(), client.getCoOwnerIds(), client.getPrefix(), client.getAltPrefix(), Activity.playing("Blind Test"), OnlineStatus.ONLINE, client.getServerInvite(), client.getSuccess(), client.getWarning(), client.getError(), null, null, (ArrayList<Command>) client.getCommands(), true, true, null, client.getHelpWord(), client.getScheduleExecutor(), 200, new AnnotatedModuleCompilerImpl(), client.getSettingsManager());
        this.blindTestChannel = blindTestChannel;
    }

    @Override
    public void onEvent(GenericEvent gevent) {
        if (blindTestChannel != null && gevent instanceof MessageReceivedEvent) {
            MessageReceivedEvent event = (MessageReceivedEvent) gevent;
            if (event.getMessage().isFromGuild() && !event.getChannel().getName().equals(blindTestChannel)) return;
        }
        super.onEvent(gevent);
    }
}