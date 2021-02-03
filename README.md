### 🎶 A poorly written blind-test bot powered by https://github.com/jagrosh/MusicBot
A collaborative blind-test Discord bot.

Everyone gives suggestions to the bot in DM, songs are added in a common pool. Every song is then picked randomly from the pool (the author of the suggestion cannot play).

Everyone types in the chat to play :   
+1 point is awarded to the first player who finds the correct artist   
+1 point is awarded to the first player who finds the correct title   
+3 point is awarded to the first player who types both answers at the same time (assuming no one found neither of them)   

There is a tolerance for typos in the analysis of answers (Levenshtein distance).

### Commands
Blind-Test DM:   
`!add <Youtube URL>` - adds song/playlist to the blindtest pool   
`!list` - list the added songs   
`!settitle <song index> <title>` - sets the song title   
`!setartist <song index> <artist>` - sets the song artist   
`!remove <song index>` - removes song from the blindtest pool   

Blind-Test public:   
`!rules` - prints the rules   
`!pool` - prints the song pool   
`!scores` - prints the scoreboard   

Blind-Test DJ:   
`!reset` - resets the whole state of the blind-test   
`!limit <maximum number of songs per player>` - updates the song limit for players   
`!backup <Backup name>` - backups the state of the game   
`!addpoint <points> <nick>` - adds points to some user's score   
`!restore <Backup name>` - restores the state of the game from a named backup   
`!lock` - lock/unlock the submissions   

DJ:   
`!next` - picks a random next song for the blindtest   
`!play <URL> or None if a songs has been paused` - plays the provided song or unpause previously paused song   
`!pause` - pauses the current song   
`!volume [0-150]` - sets or shows volume   
`!stop` - stops the current song and clears the queue   