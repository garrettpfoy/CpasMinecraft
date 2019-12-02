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
    private CpasGroupModel atLeastAdminGroup;

    /**
     * The {@link Cpas} api url.
     */
    private String apiUrl;

    /**
     * The {@link Cpas} api key.
     */
    private String apiKey;

    /**
     * The {@link Cpas} server ip.
     */
    private String serverIp;

    /**
     * The {@link Cpas} server port.
     */
    private String port;

    /**
     * True if the {@link MinecraftCpas} plugin should hijack the base minecraft ban command and replace it with its own.
     */
    private boolean overrideBanCommand;

    /**
     * The maximum number of ban history records to request from the server when using the {@link BanHistoryCommand}
     */
    private int banHistoryCount;

    /**
     * True if this plugin should assign primary groups.
     */
    private boolean usePrimaryGroups;

    /**
     * The primary groups if {@link #usePrimaryGroups()} returns true.
     */
    private Map<Object, ? extends CommentedConfigurationNode> primaryGroups;

    /**
     * True if this plugin should assign division groups.
     */
    private boolean useDivisionGroups;

    /**
     * The division groups if {@link #useDivisionGroups()} returns true.
     */
    private Map<Object, ? extends CommentedConfigurationNode> divisionGroups;

    /**
     * True if this plugin should assign secondary groups.
     */
    private boolean useSecondaryGroups;

    /**
     * The secondary groups if {@link #useSecondaryGroups()} returns true.
     */
    private Map<Object, ? extends CommentedConfigurationNode> secondaryGroups;

    /**
     * True if this plugin should assign the ds group.
     */
    private boolean useDsGroup;

    /**
     * The name of the ds group to assign.
     */
    private String dsGroup;

    /**
     * True if this plugin should use the noGroupGroup.
     */
    private boolean useNoGroup;

    /**
     * The group a player will receive if they will not receive a primary group.
     */
    private String noGroupGroup;

    /**
     * The group permission prefix.
     */
    private String groupsPrefix;

    /**
     * The cpasRankId of what admin can only be banned from >= said rank.
     */
    private int banRankThreshold;

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
        atLeastAdminGroup = new CpasGroupModel(
                config.getNode("cpas", "adminLevel", "name").getString(),
                config.getNode("cpas", "adminLevel", "rank").getInt()
        );
        apiUrl = config.getNode("cpas", "apiUrl").getString();
        apiKey = config.getNode("cpas", "apiKey").getString();
        serverIp = config.getNode("cpas", "serverIp").getString();
        port = config.getNode("cpas", "port").getString();
        overrideBanCommand = config.getNode("commands", "overrideBanCommand").getBoolean();
        banHistoryCount = config.getNode("commands", "banHistoryCount").getInt();
        usePrimaryGroups = config.getNode("cpas", "usePrimaryGroups").getBoolean();
        primaryGroups = config.getNode("cpas", "primaryGroups").getChildrenMap();
        useDivisionGroups = config.getNode("cpas", "useDivisionGroups").getBoolean();
        divisionGroups = config.getNode("cpas", "divisionGroups").getChildrenMap();
        useSecondaryGroups = config.getNode("cpas", "useSecondaryGroups").getBoolean();
        secondaryGroups = config.getNode("cpas", "secondaryGroups").getChildrenMap();
        useDsGroup = config.getNode("cpas", "useDsGroup").getBoolean();
        dsGroup = config.getNode("cpas", "dsGroup").getString();
        useNoGroup = config.getNode("cpas", "useNoGroup").getBoolean();
        noGroupGroup = config.getNode("cpas", "noGroupGroup").getString();
        groupsPrefix = config.getNode("cpas", "groupsPrefix").getString();
        banRankThreshold = config.getNode("commands", "banRankThreshold").getInt();
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
        return apiUrl;
    }

    /**
     * @return the {@link Cpas} api key.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * @return the {@link Cpas} server ip.
     */
    public String getServerIp() {
        return serverIp;
    }

    /**
     * @return the {@link Cpas} server port.
     */
    public String getPort() {
        return port;
    }

    /**
     * @return true if the {@link MinecraftCpas} plugin should hijack the base
     * minecraft ban command and replace it with its own.
     */
    public boolean shouldOverrideBanCommand() {
        return overrideBanCommand;
    }

    /**
     * @return the maximum number of ban history records to request from the server when using the {@link BanHistoryCommand}
     */
    public int numberOfBanHistoryRecords() {
        return banHistoryCount;
    }

    /**
     * @return true if this plugin should assign primary groups.
     */
    public boolean usePrimaryGroups() {
        return usePrimaryGroups;
    }

    /**
     * @return the primary groups if {@link #usePrimaryGroups()} returns true.
     */
    public Map<Object, ? extends CommentedConfigurationNode> getPrimaryGroups() {
        return primaryGroups;
    }

    /**
     * @return true if this plugin should assign division groups.
     */
    public boolean useDivisionGroups() {
        return useDivisionGroups;
    }

    /**
     * @return the division groups if {@link #useDivisionGroups()} returns true.
     */
    public Map<Object, ? extends CommentedConfigurationNode> getDivisionGroups() {
        return divisionGroups;
    }

    /**
     * @return true if this plugin should assign secondary groups.
     */
    public boolean useSecondaryGroups() {
        return useSecondaryGroups;
    }

    /**
     * @return the secondary groups if {@link #useSecondaryGroups()} returns true.
     */
    public Map<Object, ? extends CommentedConfigurationNode> getSecondaryGroups() {
        return secondaryGroups;
    }

    /**
     * @return true if this plugin should assign the ds group.
     */
    public boolean useDsGroup() {
        return useDsGroup;
    }

    /**
     * @return the name of the ds group to assign.
     */
    public String getDsGroup() {
        return dsGroup;
    }

    /**
     * @return the minimum group needed to be in to be considered an admin.
     */
    @Nonnull
    public CpasGroupModel getAtLeastAdminGroup() {
        Preconditions.checkNotNull(atLeastAdminGroup, "atLeastAdminGroup");
        return atLeastAdminGroup;
    }

    /**
     * @return true if this plugin should use the noGroupGroup.
     */
    public boolean useNoGroup() {
        return useNoGroup;
    }

    /**
     * @return the group a player will receive if they will not receive a primary group.
     */
    public String getNoGroupGroup() {
        return noGroupGroup;
    }

    /**
     * @return the group permission prefix.
     */
    public String getGroupsPrefix() {
        return groupsPrefix;
    }

    /**
     * @return the cpasRankId of what admin can only be banned from >= said rank.
     */
    public int getBanRankThreshold() {
        return banRankThreshold;
    }
}
