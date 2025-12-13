package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.*;

import java.util.List;

public class CommandMusicDebug implements ICommand {
    private final List<String> aliases;

    public CommandMusicDebug() {
        this.aliases = List.of("bmp", "musicplayer");
    }

    @Override
    public String getCommandName() {
        return "bmp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bmp <debug|reload>";
    }

    @Override
    public List getCommandAliases() {
        return this.aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("debug")) {
            printDebugInfo(sender);
        } else if (args[0].equalsIgnoreCase("reload")) {
            MusicManager.reload();
            sender.sendChatToPlayer(ChatMessageComponent.createFromText("§a[BTWMusic] Configuration and Music Packs reloaded."));
        } else {
            sender.sendChatToPlayer(ChatMessageComponent.createFromText("§cUsage: /bmp <debug|reload>"));
        }
    }

    private void printDebugInfo(ICommandSender sender) {
        MusicContext ctx = btwmusicplayerAddon.getMusicContext();
        MusicCombatTracker tracker = ctx.getCombatTracker();
        PlaylistManager playlist = ctx.getPlaylistManager();
        ConditionEvaluator evaluator = ctx.getConditionEvaluator();

        EntityPlayer player = (sender instanceof EntityPlayer) ? (EntityPlayer) sender : null;

        sender.sendChatToPlayer(ChatMessageComponent.createFromText("§6--- BTW Music Player Debug ---"));

        if (tracker == null || playlist == null) {
            sender.sendChatToPlayer(ChatMessageComponent.createFromText("§cComponents not initialized yet (Wait for SoundManager)."));
            return;
        }

        // 1. Current Song
        SongRule current = playlist.getCurrentSongRule();
        String songName = (current != null) ? "§a" + current.file : "§7None";
        String priority = (current != null) ? String.valueOf(current.priority) : "-";
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("§eCurrent Song: " + songName + " §e(Prio: " + priority + ")"));

        // 2. Combat State
        boolean inCombat = tracker.isInCombat(sender.getEntityWorld().getTotalWorldTime());
        boolean victory = tracker.isVictoryCooldownActive(sender.getEntityWorld().getTotalWorldTime());
        String combatState = inCombat ? "§cCOMBAT" : (victory ? "§bVICTORY" : "§7CALM");
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("§eState: " + combatState));

        // 3. Environment (Requires Player)
        if (player != null) {
            String rawBiome = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;
            sender.sendChatToPlayer(ChatMessageComponent.createFromText("§eBiome (Raw): §f" + rawBiome));

            String dim = (player.dimension == 0) ? "Overworld" : (player.dimension == -1 ? "Nether" : "End");
            String time = (player.worldObj.getWorldTime() % 24000 < 13000) ? "Day" : "Night";
            int y = (int) player.posY;
            sender.sendChatToPlayer(ChatMessageComponent.createFromText("§eCtx: §f" + dim + ", " + time + ", Y=" + y));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "debug", "reload");
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    public static List getListOfStringsMatchingLastWord(String[] args, String ... options) {
        String lastArg = args[args.length - 1];
        java.util.ArrayList<String> matches = new java.util.ArrayList<String>();
        for (String option : options) {
            if (option.startsWith(lastArg)) {
                matches.add(option);
            }
        }
        return matches;
    }

    @Override
    public int compareTo(Object o) {
        return this.getCommandName().compareTo(((ICommand)o).getCommandName());
    }
}