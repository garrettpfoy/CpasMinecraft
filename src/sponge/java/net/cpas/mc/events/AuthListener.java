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

import net.cpas.Cpas;
import net.cpas.mc.MinecraftCpas;
import net.cpas.model.BanInfoModel;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Listens to the {@link ClientConnectionEvent.Auth} event.
 *
 * @author agent6262
 */
public class AuthListener extends BaseEvent {

    /**
     * Creates a new {@link AuthListener} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    AuthListener(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    /**
     * Adds players to the cache and gets info on a player form CPAS.
     *
     * @param event the {@link ClientConnectionEvent.Auth} event.
     */
    @IsCancelled (Tristate.UNDEFINED)
    @Listener (order = Order.EARLY)
    public void onAuth(@Nonnull ClientConnectionEvent.Auth event) throws ExecutionException, InterruptedException {
        final UUID playerUUID = event.getProfile().getUniqueId();

        //Check if player is banned
        Cpas.getInstance().getBanInfo(playerUUID.toString(), new ProcessBanInfoResponse(pluginInstance, event))
                // Holds this thread for the callback to finish before continuing
                .get();
    }

    /**
     * Class that handles the processing of the response from the auth event
     */
    private static class ProcessBanInfoResponse implements Cpas.ProcessResponse<BanInfoModel> {

        /**
         * The {@link MinecraftCpas} instance.
         */
        private final MinecraftCpas pluginInstance;

        /**
         * The event to handle on auth.
         */
        private final ClientConnectionEvent.Auth event;

        /**
         * Creates a new {@link ProcessBanInfoResponse} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance
         * @param event          the event of the process
         */
        ProcessBanInfoResponse(@Nonnull MinecraftCpas pluginInstance, @Nonnull ClientConnectionEvent.Auth event) {
            this.pluginInstance = pluginInstance;
            this.event = event;
        }

        @Override
        public void process(BanInfoModel response, String errorMessage) {
            if (errorMessage == null && response.duration != 0) {
                final String kickText = response.duration < 0 ? "You are permanently banned from this server.\nReason: " +
                        response.reason :
                        "You are temporarily banned from this server, your ban will expire in " + response.duration + " minuets." +
                                "\nReason: " + response.reason;
                event.setCancelled(true);
                event.setMessage(Text.of(kickText));
            } else {
                //Minecraft only knows about perm bans so if you want to show time left on temp bans we need this
                final BanService banService = pluginInstance.getBanService();
                final GameProfile userProfile = event.getProfile();
                if (banService.isBanned(userProfile)) {
                    banService.getBanFor(userProfile).ifPresent(profile->{
                        final Optional<Instant> optionalInstant = profile.getExpirationDate();
                        final String kickText = optionalInstant.map(instant->
                                "You are temporarily banned from this server, your ban will expire in: " +
                                        ((instant.getEpochSecond() - Instant.now().getEpochSecond()) * 60) + " minuets" +
                                        "\nReason: " + TextSerializers.FORMATTING_CODE.serialize(profile.getReason().orElse(Text.of(""))))
                                .orElse("You are permanently banned from this server.\nReason: " +
                                        TextSerializers.FORMATTING_CODE.serialize(profile.getReason().orElse(Text.of(""))));
                        event.setCancelled(true);
                        event.setMessage(Text.of(kickText));
                    });
                }
            }
        }

        @Override
        public Class<BanInfoModel> getModelClass() {
            return BanInfoModel.class;
        }
    }
}
