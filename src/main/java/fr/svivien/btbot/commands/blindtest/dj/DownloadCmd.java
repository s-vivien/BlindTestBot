package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.blindtest.model.SongEntry;
import fr.svivien.btbot.commands.BTDJCommand;

import java.io.File;

public class DownloadCmd extends BTDJCommand {

    public DownloadCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "download";
        this.help = "local download of all the songs";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        var entries = this.blindTest.getAllEntries();
        int n = 0;
        for (var entry : entries) {
            downloadEntryMP3(entry);
            n++;
            System.err.println(n + "out of " + entries.size() + " downloaded");
        }
    }

    private void downloadEntryMP3(SongEntry entry) {
        var targetPath = blindTest.getLocalFilePath() + File.separator + entry.getYtId();
        if (new File(targetPath).exists()) {
            System.err.println(targetPath + " already exists");
            return;
        }
        try {
            Process process;

            // windows only
            process = Runtime.getRuntime()
                    .exec(String.format("cmd.exe /c yt-dlp.exe %s -x -o %s", entry.getUrl(), targetPath));

            int exitCode = process.waitFor();
            System.err.println("done with code " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
