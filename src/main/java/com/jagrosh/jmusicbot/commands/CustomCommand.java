package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.util.Objects;
import java.util.function.Predicate;

public class CustomCommand extends Command {

    @Override
    protected void execute(CommandEvent event) {
    }

    public static class Category extends Command.Category {
        public Category(String name) {
            super(name);
        }

        public Category(String name, Predicate<CommandEvent> predicate) {
            super(name, predicate);
        }

        public Category(String name, String failResponse, Predicate<CommandEvent> predicate) {
            super(name, failResponse, predicate);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Category))
                return false;
            Category other = (Category) obj;
            return Objects.equals(getName(), other.getName());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + Objects.hashCode(this.getName());
            return hash;
        }
    }
}
