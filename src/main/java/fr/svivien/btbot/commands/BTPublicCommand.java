package fr.svivien.btbot.commands;

import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;

public abstract class BTPublicCommand extends BTCommand {

    public BTPublicCommand(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.category = new Category("Blind-Test public");
    }
}
