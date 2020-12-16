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
import com.jagrosh.jmusicbot.commands.BTPublicCommand;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class BTPublicRulesCmd extends BTPublicCommand {

    private BlindTest blindTest;

    public BTPublicRulesCmd(BlindTest blindTest) {
        this.blindTest = blindTest;
        this.name = "rules";
        this.help = "prints the rules";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String rules = ":bulb: **Propositions de chansons :** :bulb:\n" +
                       "* Le pool de chansons est collaboratif, chaque participant propose un nombre égal de chansons\n" +
                       "* Pour proposer une chanson, envoyer un DM au bot au format suivant : `!add <YT url>`. Quelques secondes plus tard, le bot vous répondra en vous indiquant le succès/échec de l'opération, et la liste des chansons ajoutées jusqu'à maintenant\n" +
                       "* Chaque chanson de la liste est suffixée par ce que les gens devront taper pour valider leur réponse (artiste, titre), veillez à ce que ces données soient correctes\n" +
                       "* Si ces informations sont incorrectes ou incomplètes, vous pouvez les mettre à jour manuellement à l'aide des commandes `!setartist <id> <artist>` et `!settitle <id> <title>`\n" +
                       "\n" +
                       ":bulb: **Règles du blind-test :** :bulb:\n" +
                       "* Quand la chanson démarre, tout le monde tape ses propositions directement dans le chan général\n" +
                       "* 1 point est donné à la première personne qui trouve le titre, 1 point pour l'artiste, et 3 points si les deux sont donnés en même temps (et qu'aucun des deux n'avait été trouvé au préalable)\n" +
                       "* Si personne ne trouve ni l'artiste ni le titre, 1 point est retiré à la personne qui a proposé la chanson\n" +
                       "\n" +
                       "*Pour obtenir la liste des commandes disponibles, taper `!bthelp`*";
        commandEvent.reply(rules);
    }
}
