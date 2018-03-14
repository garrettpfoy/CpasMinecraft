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
package net.cpas.mc.events;

import net.cpas.Cpas;
import net.cpas.mc.MinecraftCpas;
import net.cpas.model.CpasGroupModel;
import net.cpas.model.InfoModel;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Listens to the {@link ClientConnectionEvent.Login} event.
 *
 * @author agent6262
 */
public class LoginListener extends BaseEvent {

    /**
     * Creates a new {@link BaseEvent} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    LoginListener(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    /**
     * Adds players to the cache and gets info on a player form cpas.
     *
     * @param event the {@link ClientConnectionEvent.Login} event.
     */
    @Listener
    public void onLogin(@Nonnull ClientConnectionEvent.Login event) {
        final UUID playerUUID = event.getProfile().getUniqueId();
        final InetSocketAddress playerAddress = event.getConnection().getAddress();
        final Optional<Player> player = event.getCause().first(Player.class);

        //Get the player info
        if (player.isPresent()) {
            Cpas.getInstance().getInfo(playerUUID.toString(), playerAddress.getAddress().getHostAddress(), false,
                    new ProcessInfoModelResponse(pluginInstance, playerUUID, player.get()));
        } else {
            pluginInstance.getLogger().info("Attempted to fire Login event for non player.");
        }
    }

    /**
     * Class that handles the processing of the response from the Login event
     */
    private static class ProcessInfoModelResponse implements Cpas.ProcessResponse<InfoModel> {

        /**
         * The permission context to set in.
         */
        private static final Set<Context> context = new HashSet<>();

        /**
         * The {@link MinecraftCpas} instance.
         */
        private final MinecraftCpas pluginInstance;

        /**
         * The uuid of the player.
         */
        private final UUID playerUUID;

        /**
         * The callback player.
         */
        private final Player player;

        /**
         * Creates a new {@link ProcessInfoModelResponse} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance.
         * @param playerUUID     The {@link UUID} of the player
         * @param player         the player {@link Optional} object
         */
        ProcessInfoModelResponse(@Nonnull MinecraftCpas pluginInstance, @Nonnull UUID playerUUID, @Nonnull Player player) {
            this.pluginInstance = pluginInstance;
            this.playerUUID = playerUUID;
            this.player = player;
        }

        /**
         * @param groups the list of groups to check if it contains a given group.
         * @param group  the group to check for.
         * @return true if the list contains the given group false otherwise.
         */
        private boolean checkContainingGroups(List<CpasGroupModel> groups, CpasGroupModel group) {
            for (CpasGroupModel cpasGroupModel : groups) {
                if (cpasGroupModel.rank == group.rank && cpasGroupModel.name.equals(group.name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void process(InfoModel response, String errorMessage) {
            if (errorMessage != null) {
                return;
            }

            final CpasGroupModel atLeastAdminGroup = pluginInstance.getConfig().getAtLeastAdminGroup();
            // Add admin to admin list
            if (checkContainingGroups(response.groups, atLeastAdminGroup)) {
                pluginInstance.getAdminPlayerCache().add(response);
            }
            // Init stuff
            final PermissionService permissionService = pluginInstance.getPermissionService();
            final Optional<Subject> optionalSubject = permissionService.getUserSubjects().getSubject(playerUUID.toString());
            // When SubjectCollections are queried for a Subject they will automatically be created, if they do not
            // already exist. However they might not necessarily show up in getAllSubjects() unless none-default values
            // are set. If for some odd reason A user is not present log ad return;
            if (!optionalSubject.isPresent()) {
                pluginInstance.getLogger().warn("Subject data not created, please contact plugin maintainer.");
                return;
            }
            final SubjectData playerData = optionalSubject.get().getSubjectData();
            final String groupsText = pluginInstance.getConfig().getGroupsPrefix();

            // Remove any other primary groups and add primary group just in case
            if (pluginInstance.getConfig().usePrimaryGroups()) {
                // Remove any other primary groups
                final Map<Object, ? extends CommentedConfigurationNode> primaryGroups = pluginInstance.getConfig().getPrimaryGroups();
                for (Map.Entry<Object, ? extends CommentedConfigurationNode> kvp : primaryGroups.entrySet()) {
                    // No point in not removing the primary group because it will just get added again anyway.
                    // Also you can not check if a player has a parent no hasParent() function.
                    playerData.setPermission(context, groupsText + kvp.getValue().getString(), Tristate.UNDEFINED);
                }
                // Add primary group just in case
                final String strRank = String.valueOf(response.primaryGroup.rank);
                if (primaryGroups.containsKey(strRank)) {
                    playerData.setPermission(context, groupsText + primaryGroups.get(strRank).getString(), Tristate.TRUE);
                    // Remove 'noGroup' if there is a primary group assigned
                    if (pluginInstance.getConfig().useNoGroup()) {
                        final String noGroupGroup = pluginInstance.getConfig().getNoGroupGroup();
                        playerData.setPermission(context, groupsText + noGroupGroup, Tristate.UNDEFINED);
                    }
                } else {
                    // Add 'noGroup' if there is no primary group assigned
                    if (pluginInstance.getConfig().useNoGroup()) {
                        final String noGroupGroup = pluginInstance.getConfig().getNoGroupGroup();
                        playerData.setPermission(context, groupsText + noGroupGroup, Tristate.TRUE);
                    }
                }
            }
            // Remove any other division groups and add division group just in case
            if (pluginInstance.getConfig().useDivisionGroups()) {
                final Map<Object, ? extends CommentedConfigurationNode> divisionGroups = pluginInstance.getConfig().getDivisionGroups();
                for (Map.Entry<Object, ? extends CommentedConfigurationNode> kvp : divisionGroups.entrySet()) {
                    // No point in not removing the division group because it will just get added again anyway.
                    // Also you can not check if a player has a parent, no hasParent() function.
                    playerData.setPermission(context, groupsText + kvp.getValue().getString(), Tristate.UNDEFINED);
                }
                // Add division group just in case
                if (divisionGroups.containsKey(response.division)) {
                    playerData.setPermission(context, groupsText + divisionGroups.get(response.division).getString(), Tristate.TRUE);
                }
            }
            // Remove any other secondary groups and add secondary groups just in case
            if (pluginInstance.getConfig().useSecondaryGroups()) {
                final Map<Object, ? extends CommentedConfigurationNode> secondaryGroups = pluginInstance.getConfig().getSecondaryGroups();
                for (Map.Entry<Object, ? extends CommentedConfigurationNode> kvp : secondaryGroups.entrySet()) {
                    // If you have a group you are not supposed to have it will remove it
                    // else it will add / re-add any group you should have.
                    if (isMemberOfGroup(response, Integer.valueOf(kvp.getKey().toString()))) {
                        playerData.setPermission(context, groupsText + kvp.getValue().getString(), Tristate.TRUE);
                    } else {
                        playerData.setPermission(context, groupsText + kvp.getValue().getString(), Tristate.UNDEFINED);
                    }
                }
            }
            //Set and remove ds
            if (pluginInstance.getConfig().useDsGroup()) {
                if (response.dsInfo.isDedicatedSupporter) {
                    playerData.setPermission(context, groupsText + pluginInstance.getConfig().getDsGroup(), Tristate.TRUE);
                } else {
                    playerData.setPermission(context, groupsText + pluginInstance.getConfig().getDsGroup(), Tristate.UNDEFINED);
                }

                pluginInstance.getGame().getServer().getBroadcastChannel().send(
                        Text.of(TextColors.YELLOW, player.getName() + " has joined the game" + (response.dsInfo.isDedicatedSupporter ? " : " + response.dsInfo.joinMessage : "")));
            }
        }

        /**
         * Checks if the given {@link InfoModel} contains a group with the given rank
         *
         * @param response the {@link InfoModel} to compare to
         * @param rank     the rank to check
         * @return true if the {@link InfoModel} contains the given rank
         */
        private boolean isMemberOfGroup(InfoModel response, int rank) {
            for (CpasGroupModel group : response.groups) {
                if (group.rank == rank)
                    return true;
            }
            return false;
        }

        @Override
        public Class<InfoModel> getModelClass() {
            return InfoModel.class;
        }
    }
}
