package net.cpas.mc.commands;

import lombok.NonNull;
import net.cpas.Cpas;
import net.cpas.mc.main.Instance;
import net.cpas.mc.main.MinecraftCpas;
import net.cpas.model.InfoModel;
import net.cpas.model.SuccessResponseModel;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleCommand;

import java.util.Arrays;
import java.util.UUID;

public class BanCommand extends SimpleCommand {

    private MinecraftCpas instance;

    public BanCommand() {
        super("ban");
        setMinArguments(3);
        setDescription("Command used to ban players using MAUL integration");
        setArg(0, "Player");
        setArg(1, "Time");
        setArg(2, "Reason");
        setUsage("/ban <player> <time> <reason>");
        setPermission("cpas.ban");

        Instance instanceClass = new Instance();
        this.instance = instanceClass.getInstance();
    }

    @Override
    public void onCommand() {
        Player banner = getPlayer();
        OfflinePlayer bannedOffline = Bukkit.getOfflinePlayer(args[0]);
        Player banned = bannedOffline.getPlayer();

        StringBuilder reasonTemp = new StringBuilder();

        for(int i = 2; i < args.length; i++) {
            reasonTemp.append(" ").append(args);
        }

        String reason = reasonTemp.toString();

        String[] admins = getAdminList();

        int duration = Integer.parseInt(args[1]);

        if(checkBanRules(banner, banned)) {
            Cpas.getInstance().banUser(
                    banned.getUniqueId().toString(),
                    banned.getName(),
                    banner.getUniqueId().toString(),
                    admins,
                    duration,
                    reason,
                    new ProcessBanResponse(instance, banner, banned, reason, duration)
            );

            Common.tell(banner, "&cMAUL &8\u00BB &7Player has successfully been banned");
        }

        else {
            Common.tell(banner, "&cMAUL &8\u00BB &7That ban didn't go through! You either can't ban that user, or MAUL is not initialized correctly");
        }



    }
    private String[] getAdminList() {
        int current = 0;
        String[] admins = new String[10];
        Arrays.fill(admins, "");
        for(InfoModel infoModel : instance.getAdminPlayerCache()) {
            if(current < 10) {
                admins[current++] = infoModel.gameId.toString();
            }
            else {
                break;
            }
        }
        return admins;
    }

    private boolean checkBanRules(Player admin, Player player) {
        InfoModel adminInfoModel = null;
        InfoModel playerInfoModel = null;

        for(InfoModel infoModel : instance.getAdminPlayerCache()) {
            if(infoModel.gameId.equals(admin.getUniqueId())) {
                adminInfoModel = infoModel;
            }
            if(infoModel.gameId.equals(player.getUniqueId())) {
                playerInfoModel = infoModel;
            }
        }
        if(playerInfoModel == null) {
            return true;
        }
        else if(adminInfoModel == null) {
            return false;
        }
        final int cutOff = instance.retrieveConfig().getBanRankThreshold();
        if(playerInfoModel.primaryGroup.rank >= cutOff && adminInfoModel.primaryGroup.rank < cutOff) {
            return false;
        }
        return true;
    }

    private static class ProcessBanResponse implements Cpas.ProcessResponse<SuccessResponseModel> {
        private final MinecraftCpas instance;
        private final Player admin;
        private final Player player;
        private final String reason;
        private final int duration;

        public ProcessBanResponse(@NonNull MinecraftCpas instance, @NonNull Player admin, @NonNull Player player, @NonNull String reason, int duration) {
            this.instance = instance;
            this.admin = admin;
            this.player = player;
            this.reason = reason;
            this.duration = duration;
        }

        @Override
        public void process(SuccessResponseModel successResponseModel, String errorResponse) {
            if(errorResponse != null || !successResponseModel.success) {
                instance.getLogger().warning("Ban was not executed correctly! Admin: " + admin.getDisplayName() + " | Banned: " + player.getDisplayName());
                Common.tell(admin, "&cMAUL &8\u00BB &7That ban didn't go through, please contact Tech to fix it. In the meantime, use a local ban (essentials) on the hub.");
            }

            String kickMessage = Common.colorize("&cYou were banned from this server, please reconnect for more information.");
            player.kickPlayer(kickMessage);

        }

        @Override
        public Class<SuccessResponseModel> getModelClass() {
            return SuccessResponseModel.class;
        }

    }

}
