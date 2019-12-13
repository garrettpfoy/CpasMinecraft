package net.cpas.mc.listeners;

import net.cpas.mc.main.Instance;
import net.cpas.mc.main.MinecraftCpas;
import net.cpas.model.InfoModel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class onDisconnect implements Listener {

    private MinecraftCpas instance;

    public onDisconnect() {
        Instance temp = new Instance();
        this.instance = temp.getInstance();
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        if(player.isOnline()) {
            final InfoModel adminInfoModel = instance.getPlayerInfoModel(player.getUniqueId());
            if(adminInfoModel != null) {
                instance.getAdminPlayerCache().remove(adminInfoModel);
            }
        }
    }

}
