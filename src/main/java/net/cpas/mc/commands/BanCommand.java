/*
 * Copyright (c) 2017, Tyler Bucher
 * Copyright (c) 2017, Orion Stanger
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
package net.cpas.mc.commands;

import net.cpas.Cpas;
import net.cpas.mc.MinecraftCpas;
import net.cpas.model.InfoModel;
import net.cpas.model.SuccessResponseModel;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Arrays;

/**
 * Handle the /cpas ban (or /ban if the configuration allows) command
 *
 * @author oey192
 */
public class BanCommand extends BaseCommand {

    /**
     * The total number of admins to get for the ban command.
     */
    private static final int NUMBER_OF_ADMINS = 10;

    /**
     * Creates a new {@link BanCommand} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    BanCommand(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    /**
     * @return the top 10 current admins on the server
     */
    private String[] getAdminList() {
        int current = 0;
        String[] admins = new String[NUMBER_OF_ADMINS];
        Arrays.fill(admins, "");
        for (InfoModel infoModel : pluginInstance.getAdminPlayerCache()) {
            if (current < NUMBER_OF_ADMINS) {
                admins[current++] = infoModel.gameId.toString();
            } else {
                break;
            }
        }
        return admins;
    }

    /**
     * @param src the user who is banning.
     * @param user  the user who is being banned.
     * @return true if the admin can ban the user.
     */
    private boolean checkBanRules(CommandSource src, User user) {
        // If console is banning just accept
        if (!(src instanceof User)) {
            return true;
        }
        final User adminUser = (User) src;
        // Grab the admin and user info
        InfoModel adminInfoModel = null;
        InfoModel userInfoModel = null;
        for (InfoModel infoModel : pluginInstance.getAdminPlayerCache()) {
            if(infoModel.gameId.equals(adminUser.getUniqueId())) {
                adminInfoModel = infoModel;
            }
            // If user is banning self
            if(infoModel.gameId.equals(user.getUniqueId())) {
                userInfoModel = infoModel;
            }
        }
        // If user is null i.e pub yes; if admin is null heck no
        if(userInfoModel == null) {
            return true;
        } else if(adminInfoModel == null) {
            return false;
        }
        // Don't ban if user is higher rank than cut off.
        final int cutOff = pluginInstance.getConfig().getBanRankThreshold();
        if(userInfoModel.primaryGroup.rank >= cutOff && adminInfoModel.primaryGroup.rank < cutOff) {
            return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public CommandResult execute(@Nonnull CommandSource src, @Nonnull CommandContext args) throws CommandException {
        final String reason = castArgument(args, "reason", String.class);
        final User user = castArgument(args, "user", User.class);

        // Check ban rules first
        if(!checkBanRules(src, user)) {
            src.sendMessage(Text.of(TextColors.RED, "You can not ban that user."));
            return CommandResult.success();
        }
        src.sendMessage(Text.of(TextColors.GRAY, "Sending ban to server..."));
        final int duration = castArgument(args, "duration", Integer.class);
        // If we can't get the Game ID from the banner then we can pass "" for this field. The API call will use the
        // default admin specified in its config as the banning admin
        final String bannerId = (src instanceof User ? ((User) src).getUniqueId().toString() : "");
        Cpas.getInstance().banUser(
            user.getUniqueId().toString(),
            user.getName(),
            bannerId,
            getAdminList(),
            duration,
            reason,
            new ProcessBanResponse(pluginInstance, src, user, reason, duration));

        // Unfortunately we can't guarantee success at this point since the the result of the call will be sent to the
        // user asynchronously, but most of the time it should succeed, and if it doesn't it will print an error message
        // to the user anyway so we may as well mark it as having succeeded.
        return CommandResult.success();
    }

    /**
     * Class that handles the processing of the response from the ban command
     */
    private static class ProcessBanResponse implements Cpas.ProcessResponse<SuccessResponseModel> {

        private final MinecraftCpas pluginInstance;
        private final CommandSource src;
        private final User user;
        private final String reason;
        private final int duration;

        /**
         * Creates a new {@link ProcessBanResponse} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance.
         * @param src            the sender of the command
         * @param user           the {@link User} that was banned
         * @param reason         the reason that the {@link User} was banned
         * @param duration       the duration that the {@link User} was banned
         */
        ProcessBanResponse(@Nonnull MinecraftCpas pluginInstance, @Nonnull CommandSource src, @Nonnull User user, @Nonnull String reason, int duration) {
            this.pluginInstance = pluginInstance;
            this.src = src;
            this.user = user;
            this.reason = reason;
            this.duration = duration;
        }

        @Override
        public void process(SuccessResponseModel successResponseModel, String errorResponse) {
            if (errorResponse != null || !successResponseModel.success) {
                pluginInstance.getLogger().error(String.format(
                    "%s attempted to ban %s which resulted in the error: %s",
                    src.getName(),
                    user.getUniqueId(),
                    (errorResponse == null ? "failure" : errorResponse)));

                // Attempt to ban locally
                src.sendMessage(Text.of(TextColors.GRAY, "Failed to ban on CPAS, Attempting to ban locally..."));
                final BanService banService = pluginInstance.getGame().getServiceManager().provideUnchecked(BanService.class);
                //Minecraft has a bug where the start time is always the same as the end time on a temp ban
                final Instant start = Instant.now();
                final Instant end = Instant.now().plusSeconds(duration * 60);
                final Ban userLocalBan = duration == 0
                    ? Ban.builder()
                    .type(BanTypes.PROFILE)
                    .profile(user.getProfile())
                    .startDate(start)
                    .reason(Text.of(reason))
                    .source(src)
                    .build()
                    : Ban.builder()
                    .type(BanTypes.PROFILE)
                    .profile(user.getProfile())
                    .startDate(start)
                    .reason(Text.of(reason))
                    .source(src)
                    .expirationDate(end)
                    .build();
                banService.addBan(userLocalBan);
            }

            // If the banned user is online, kick them.
            if (user instanceof Player) {
                ((Player) user).kick(Text.of(reason));
            }
            src.sendMessage(Text.of(TextColors.GOLD, user.getName() + " banned successfully"));
        }

        @Override
        public Class<SuccessResponseModel> getModelClass() {
            return SuccessResponseModel.class;
        }
    }
}
