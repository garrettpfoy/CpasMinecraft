package net.cpas.mc.storage;

import net.cpas.Cpas;
import net.cpas.model.CpasGroupModel;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.settings.YamlConfig;

public class Config extends YamlConfig {

    private CpasGroupModel atLeastAdminGroup;
    private String apiUrl;
    private String apiKey;
    private String serverIP;
    private String port;
    private String groupsPrefix;
    private boolean usePrimaryGroups;
    private SerializedMap primaryGroups;
    private boolean useNoGroup;
    private String noGroupGroup;
    private boolean useDivisionGroups;
    private SerializedMap divisionGroups;
    private boolean useSecondaryGroups;
    private SerializedMap secondaryGroups;
    private boolean useDsGroup;
    private String dsGroup;
    private SerializedMap adminLevel;
    private boolean overrideBanCommand;
    private int banHistoryCount;
    private int banRankThreshold;

    public Config() {
        //Loads a new Configuration file. If it doesn't exist, it will create a new one
        //that follows the default defined in resources/config.yml
        loadConfiguration("config.yml", "config.yml");
    }

    @Override
    public void onLoadFinish() {
        //This runs as soon as the configuration is done loading
        //will be used to set variables and such
        atLeastAdminGroup = new CpasGroupModel(
                getString("CPAS.adminLevel.name"),
                getInteger("CPAS.adminLevel.rank")
        );
        apiUrl = getString("CPAS.apiUrl");
        apiKey = getString("CPAS.apiKey");
        serverIP = getString("CPAS.serverIP");
        port = getString("CPAS.port");
        groupsPrefix = getString("CPAS.groupsPrefix");
        usePrimaryGroups = getBoolean("CPAS.usePrimaryGroups");
        primaryGroups = getMap("CPAS.primaryGroups");
        useNoGroup = getBoolean("CPAS.useNoGroup");
        noGroupGroup = getString("CPAS.noGroupGroup");
        useDivisionGroups = getBoolean("CPAS.useDivisionGroups");
        divisionGroups = getMap("CPAS.divisionGroups");
        useSecondaryGroups = getBoolean("CPAS.useSecondaryGroups");
        secondaryGroups = getMap("CPAS.secondaryGroups");
        useDsGroup = getBoolean("CPAS.useDsGroup");
        dsGroup = getString("CPAS.dsGroup");
        overrideBanCommand = getBoolean("commands.overrideBanCommand");
        banHistoryCount = getInteger("commands.banHistoryCount");
        banRankThreshold = getInteger("commands.banRankThreshold");
    }

    public void configCpas() {
        Cpas.getInstance().configure(getApiUrl(), getApiKey(), getServerIP(), getPort());
    }

    public CpasGroupModel getAtLeastAdminGroup() {
        return atLeastAdminGroup;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getServerIP() {
        return serverIP;
    }

    public String getPort() {
        return port;
    }

    public String getGroupsPrefix() {
        return groupsPrefix;
    }

    public boolean isUsePrimaryGroups() {
        return usePrimaryGroups;
    }

    public SerializedMap getPrimaryGroups() {
        return primaryGroups;
    }

    public boolean isUseNoGroup() {
        return useNoGroup;
    }

    public String getNoGroupGroup() {
        return noGroupGroup;
    }

    public boolean isUseDivisionGroups() {
        return useDivisionGroups;
    }

    public SerializedMap getDivisionGroups() {
        return divisionGroups;
    }

    public boolean isUseSecondaryGroups() {
        return useSecondaryGroups;
    }

    public SerializedMap getSecondaryGroups() {
        return secondaryGroups;
    }

    public boolean isUseDsGroup() {
        return useDsGroup;
    }

    public String getDsGroup() {
        return dsGroup;
    }

    public SerializedMap getAdminLevel() {
        return adminLevel;
    }

    public boolean isOverrideBanCommand() {
        return overrideBanCommand;
    }

    public int getBanHistoryCount() {
        return banHistoryCount;
    }

    public int getBanRankThreshold() {
        return banRankThreshold;
    }
}
