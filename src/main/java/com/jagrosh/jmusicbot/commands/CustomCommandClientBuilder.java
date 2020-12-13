package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;

public class CustomCommandClientBuilder extends CommandClientBuilder {

    private String blindTestChannel;

    public CustomCommandClientImpl build() {
        CommandClient client = super.build();
        CustomCommandClientImpl cclient = new CustomCommandClientImpl(client, blindTestChannel);
        if (client.getListener() != null) { cclient.setListener(client.getListener()); }
        return cclient;
    }

    public CustomCommandClientBuilder setBlindTestChannel(String blindTestChannel) {
        this.blindTestChannel = blindTestChannel;
        return this;
    }
}
