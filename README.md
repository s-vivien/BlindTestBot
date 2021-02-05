### 🎶 A poorly written blind-test bot powered by https://github.com/jagrosh/MusicBot
A collaborative blind-test Discord bot.

Anyone can give Youtube links to the bot in DM, metadata are scrapped and the songs are added in a common hidden pool. Once the game starts, the bot picks a random song from the pool, joins the voice channel and starts playing it. Everyone types their guesses in the chat and the bot analyses the answers (there is a tolerance for typos, using Damerau-Levenshtein distance), giving points to the first players that finds the correct ones.

Title and artist are scrapped from Youtube, but can be modified manually. Extra inputs to guess can also be added, like the name of a movie or a video-game, the name of the producer, etc

There are commands to see the scoreboard, the state of the pool, make/restore a backup of the game state, etc.

## Setup

[Youtube-DL](https://youtube-dl.org/) must be present in the PATH for the metadata scrapping to work correctly   
The setup process can be found [here](https://github.com/jagrosh/MusicBot/wiki/Setup)
