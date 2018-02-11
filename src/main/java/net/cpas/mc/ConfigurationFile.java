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
package net.cpas.mc;

import net.cpas.Cpas;
import net.cpas.mc.commands.BanHistoryCommand;
import com.google.common.base.Preconditions;
import net.cpas.model.CpasGroupModel;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.asset.Asset;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

/**
 * This is a wrapper for the base sponge {@link CommentedConfigurationNode}. We do this in-case
 * we change a node configuration in the config, so we do not need to change it in 10+ places.
 */
public class ConfigurationFile {

    /**
     * Holds the singleton instance for {@link ConfigurationFile}.
     */
    private static class SingletonHolder {

        /**
         * The {@link ConfigurationFile} singleton instance.
         */
        private static final ConfigurationFile INSTANCE = new ConfigurationFile();
    }

    /**
     * The CPAS plugin instance.
     */
    private MinecraftCpas pluginInstance;

    /**
     * States if the config was loaded correctly.
     */
    private boolean isConfigLoadedCorrectly = false;

    /**
     * The actual configuration file given from sponge.
     */
    private CommentedConfigurationNode config;

    /**
     * Minimum group needed to be admin.
     */
    private CpasGroupModel atleastAdminGroup;

    /**
     * Creates a new {@link ConfigurationFile} object.
     */
    private ConfigurationFile() {
    }

    /**
     * @return the {@link ConfigurationFile} singleton instance.
     */
    public static ConfigurationFile getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initializes the Config object for the main plugin object. Note that you should only call this once per server lifecycle.
     *
     * @param pluginInstance The CPAS plugin instance.
     */
    public void initialize(@Nonnull MinecraftCpas pluginInstance) {
        Preconditions.checkNotNull(pluginInstance, "pluginInstance");
        if (this.pluginInstance != null) {
            throw new IllegalStateException("ConfigurationFile attempted to be initialized twice");
        }
        this.pluginInstance = pluginInstance;
    }

    /**
     * Loads or reloads the CommentedConfigurationNode object.
     *
     * @return True if it was loaded correctly false otherwise.
     */
    public boolean loadConfig() {
        throwIfNotInitialized();
        try {
            if (Files.notExists(pluginInstance.getDefaultConfig())) {
                final Optional<Asset> optionalAsset = pluginInstance.getPluginContainer().getAsset("cpas.conf");
                if (optionalAsset.isPresent()) {
                    optionalAsset.get().copyToFile(pluginInstance.getDefaultConfig());
                }
            }
        } catch (IOException e) {
            pluginInstance.getLogger().warn("Unable to create default config file", e);
            isConfigLoadedCorrectly = false;
            return false;
        }

        try {
            config = pluginInstance.getConfigLoader().load();
        } catch (IOException e) {
            pluginInstance.getLogger().warn("Unable to load config", e);
            isConfigLoadedCorrectly = false;
            return false;
        }

        isConfigLoadedCorrectly = !config.isVirtual();
        if (isConfigLoadedCorrectly) {
            configureCpasApi();
            updateFunctionFields();
        }
        return true;
    }

    /**
     * Updates the fields for some functions.
     */
    private void updateFunctionFields() {
        atleastAdminGroup = new CpasGroupModel(
            config.getNode("cpas", "adminLevel", "name").getString(),
            config.getNode("cpas", "adminLevel", "rank").getInt());
    }

    /**
     * Configures the {@link Cpas} api library information.
     */
    public void configureCpasApi() {
        throwIfNotInitialized();
        Cpas.getInstance().configure(getApiUrl(), getApiKey(), getServerIp(), getPort());
    }

    /**
     * @throws IllegalStateException if the {@link MinecraftCpas} instance is not set.
     */
    private void throwIfNotInitialized() {
        if (pluginInstance == null) {
            throw new IllegalStateException("ConfigurationFile not initialized");
        }
    }

    /**
     * @return the {@link Cpas} api url.
     */
    public String getApiUrl() {
        return config.getNode("cpas", "apiUrl").getString();
    }

    /**
     * @return the {@link Cpas} api key.
     */
    public String getApiKey() {
        return config.getNode("cpas", "apiKey").getString();
    }

    /**
     * @return the {@link Cpas} server ip.
     */
    public String getServerIp() {
        return config.getNode("cpas", "serverIp").getString();
    }

    /**
     * @return the {@link Cpas} server port.
     */
    public String getPort() {
        return config.getNode("cpas", "port").getString();
    }

    /**
     * @return true if the {@link MinecraftCpas} plugin should hijack the base
     * minecraft ban command and replace it with its own.
     */
    public boolean shouldOverrideBanCommand() {
        return config.getNode("commands", "overrideBanCommand").getBoolean();
    }

    /**
     * @return the maximum number of ban history records to request from the server when using the {@link BanHistoryCommand}
     */
    public int numberOfBanHistoryRecords() {
        return config.getNode("commands", "banHistoryCount").getInt();
    }

    /**
     * @return true if this plugin should assign primary groups.
     */
    public boolean usePrimaryGroups() {
        return config.getNode("cpas", "usePrimaryGroups").getBoolean();
    }

    /**
     * @return the primary groups if {@link #usePrimaryGroups()} returns true.
     *
     * @throws ObjectMappingException if there is an issue mapping the config list to strings.
     */
    public Map<Object, ? extends CommentedConfigurationNode> getPrimaryGroups() throws ObjectMappingException {
        return config.getNode("cpas", "primaryGroups").getChildrenMap();
    }

    /**
     * @return true if this plugin should assign division groups.
     */
    public boolean useDivisionGroups() {
        return config.getNode("cpas", "useDivisionGroups").getBoolean();
    }

    /**
     * @return the division groups if {@link #useDivisionGroups()} returns true.
     *
     * @throws ObjectMappingException if there is an issue mapping the config list to strings.
     */
    public Map<Object, ? extends CommentedConfigurationNode> getDivisionGroups() throws ObjectMappingException {
        return config.getNode("cpas", "divisionGroups").getChildrenMap();
    }

    /**
     * @return true if this plugin should assign secondary groups.
     */
    public boolean useSecondaryGroups() {
        return config.getNode("cpas", "useSecondaryGroups").getBoolean();
    }

    /**
     * @return the secondary groups if {@link #useSecondaryGroups()} returns true.
     *
     * @throws ObjectMappingException if there is an issue mapping the config list to strings.
     */
    public Map<Object, ? extends CommentedConfigurationNode> getSecondaryGroups() throws ObjectMappingException {
        return config.getNode("cpas", "secondaryGroups").getChildrenMap();
    }

    /**
     * @return true if this plugin should assign the ds group.
     */
    public boolean useDsGroup() {
        return config.getNode("cpas", "useDsGroup").getBoolean();
    }

    /**
     * @return the name of the ds group to assign.
     */
    public String getDsGroup() {
        return config.getNode("cpas", "dsGroup").getString();
    }

    /**
     * @return the minimum group needed to be in to be considered an admin.
     */
    @Nonnull
    public CpasGroupModel getAtleastAdminGroup() {
        Preconditions.checkNotNull(atleastAdminGroup, "atleastAdminGroup");
        return atleastAdminGroup;
    }

    /**
     * @return true if this plugin should use the noGroupGroup.
     */
    public boolean useNoGroup() {
        return config.getNode("cpas", "useNoGroup").getBoolean();
    }

    /**
     * @return the group a player will receive if they will not receive a primary group.
     */
    public String getNoGroupGroup() {
        return config.getNode("cpas", "noGroupGroup").getString();
    }

    /**
     * @return the group permission prefix.
     */
    public String getGroupsPrefix() {
        return config.getNode("cpas", "groupsPrefix").getString();
    }

    /**
     * @return the cpasRankId of what admin can only be banned from >= said rank.
     */
    public int getBanRankThreshold() {
        return config.getNode("commands", "banRankThreshold").getInt();
    }
}
