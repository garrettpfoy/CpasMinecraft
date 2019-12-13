package net.cpas.mc.commands;

import lombok.NonNull;
import net.cpas.Cpas;
import net.cpas.mc.main.Instance;
import net.cpas.mc.main.MinecraftCpas;
import net.cpas.model.BanHistoryModel;
import net.cpas.model.CpasBanModel;
import net.cpas.model.SuccessResponseModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleCommand;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BanHistoryCommand extends SimpleCommand {

    private MinecraftCpas instance;

    public BanHistoryCommand() {
        super("history");
        setMinArguments(2);
        setDescription("MAUL integreted ban history information");
        setUsage("/history <playerName> <entries>");

        Instance temp = new Instance();
        instance = temp.getInstance();
    }


    @Override
    public void onCommand() {
        String playerName = args[0];
        int entries = Integer.parseInt(args[1]);
        Player player = Bukkit.getPlayer(playerName);

        Cpas.getInstance().getBanHistory(
                player.getUniqueId().toString(),
                entries,
                new ProcessBanHistoryResponse(instance, getPlayer(), player)
        );

        Common.tell(getPlayer(), "&8&l-------------------------------------");
    }

    public static class ProcessBanHistoryResponse implements Cpas.ProcessResponse<BanHistoryModel> {
        private MinecraftCpas instance;
        private Player admin;
        private Player player;

        ProcessBanHistoryResponse(@NonNull MinecraftCpas instance, @NonNull Player admin, @NonNull Player player) {
            this.instance = instance;
            this.admin = admin;
            this.player = player;
        }

        @Override
        public void process(BanHistoryModel banHistoryModel, String errorResponse) {
            if(errorResponse != null) {
                instance.getLogger().warning("Requesting ban history command failed! See errors.txt for more details");
                Common.tell(admin, "&cMAUL &8\u00BB &7Error! There was a problem with fetching that player's ban history... please contact tech!");
                return;
            }

            if(banHistoryModel.bans.isEmpty()) {
                Common.tell(admin, "&cMAUL &8\u00BB &7There is no ban history for that player");
                return;
            }

            Common.tell(admin, "&8&l--------------[&r &cMAUL &7History &8&l]--------------");
            Common.tell(admin, "&7");

            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            for(CpasBanModel ban : banHistoryModel.bans) {
                final Date date = new Date(ban.banDateSeconds * 1000L);
                final String formattedDate = formatter.format(date);

                if(ban.duration > 0) {
                    Common.tell(admin, "&c" + player.getName() + "&7 was banned for &c" + ban.length + "&7 minutes. There are: &c" + ban.duration + "&7minutes remaining.");
                    Common.tell(admin, "&cReason: &7&o" + ban.reason);
                    Common.tell(admin, "&7");
                }
                else if(ban.duration < 0) {
                    Common.tell(admin, "&c" + player.getName() + "&7was &c&opermanently &7banned.");
                    Common.tell(admin, "&cReason: &7&o" + ban.reason);
                    Common.tell(admin, "&7");
                }
                else if(ban.duration == 0) {
                    Common.tell(admin, "&c" + player.getName() + " &7was banned for &c" + ban.length + "&7 minutes.");
                    Common.tell(admin, "&cReason: &7&o" + ban.reason);
                    Common.tell(admin, "&7");
                }
            }

        }

        @Override
        public Class<BanHistoryModel> getModelClass() {
            return BanHistoryModel.class;
        }



    }

}
