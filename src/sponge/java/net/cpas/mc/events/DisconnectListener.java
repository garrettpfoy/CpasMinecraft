/*
 * Copyright (c) 2017, Tyler Bucher
 * Copyright (c) 2017, Orion Stanger
 * Copyright (c) 2019, (Contributors)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.cpas.mc.events;

import net.cpas.mc.MinecraftCpas;
import net.cpas.model.InfoModel;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Listens to the {@link ClientConnectionEvent.Disconnect} event.
 *
 * @author agent6262
 */
public class DisconnectListener extends BaseEvent {

    /**
     * Creates a new {@link DisconnectListener} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    DisconnectListener(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    /**
     * Removes players from the cache when they disconnect.
     *
     * @param event the {@link ClientConnectionEvent.Disconnect} event.
     */
    @Listener
    public void onDisconnect(@Nonnull ClientConnectionEvent.Disconnect event) {
        final Optional<Player> player = event.getCause().first(Player.class);
        if (player.isPresent()) {
            final InfoModel adminInfoModel = pluginInstance.getPlayerInfoModel(player.get().getUniqueId());
            if (adminInfoModel != null) {
                pluginInstance.getAdminPlayerCache().remove(adminInfoModel);
            }
        }
    }
}
