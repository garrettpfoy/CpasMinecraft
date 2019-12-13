package net.cpas.mc.commands;

import lombok.NonNull;
import net.cpas.Cpas;
import net.cpas.mc.main.Instance;
import net.cpas.mc.main.MinecraftCpas;
import net.cpas.model.InfoModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleCommand;

public class InfoCommand extends SimpleCommand {

    private MinecraftCpas instance;

    public InfoCommand() {
        super("info");
        setMinArguments(1);
        setDescription("Gets a player's information");
        setUsage("/info [player]");

        Instance temp = new Instance();
        instance = temp.getInstance();
    }

    @Override
    public void onCommand() {
        Player sender = getPlayer();
        Player target = Bukkit.getPlayer(args[0]);

        Cpas.getInstance().getInfo(target.getUniqueId().toString(), false, new ProcessInfoResponse(instance, sender, target));
    }

    private static class ProcessInfoResponse implements Cpas.ProcessResponse<InfoModel> {

        private final MinecraftCpas instance;
        private final Player sender;
        private final Player target;

        ProcessInfoResponse(@NonNull MinecraftCpas instance, @NonNull Player sender, @NonNull Player target) {
            this.instance = instance;
            this.sender = sender;
            this.target = target;
        }

        @Override
        public void process(InfoModel infoModel, String errorResponse) {
            if(errorResponse != null) {
                instance.getLogger().warning("Oh no! User information could not be generated!");
                return;
            }

            Common.tell(sender, "&8&l---------------[ &c" + sender.getName() + " &7Info &8&l]---------------");
            if(infoModel.userId > 0) {
                Common.tell(sender, "&7");
                Common.tell(sender, "&cForum Name: &7" + infoModel.forumName);
                Common.tell(sender, "&cRank: &7" + infoModel.primaryGroup.name);
                Common.tell(sender, "&cDivision: &7" + infoModel.divisionName);
                Common.tell(sender, "Dedicated Supporter: &7" + infoModel.dsInfo.isDedicatedSupporter);
                Common.tell(sender, "&7");
                Common.tell(sender, "&8&l---------------------------------------------");
            }
        }

        @Override
        public Class<InfoModel> getModelClass() {
            return InfoModel.class;
        }

    }

}
