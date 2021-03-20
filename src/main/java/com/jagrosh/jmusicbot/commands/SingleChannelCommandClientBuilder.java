package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;

public class SingleChannelCommandClientBuilder extends CommandClientBuilder {

    private String blindTestChannel;

    public SingleChannelCommandClientImpl build() {
        CommandClient client = super.build();
        SingleChannelCommandClientImpl cclient = new SingleChannelCommandClientImpl(client, blindTestChannel);
        if (client.getListener() != null) { cclient.setListener(client.getListener()); }
        return cclient;
    }

    public SingleChannelCommandClientBuilder setBlindTestChannel(String blindTestChannel) {
        this.blindTestChannel = blindTestChannel;
        return this;
    }
}
