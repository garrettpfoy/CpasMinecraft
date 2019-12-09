package net.cpas.mc.main;

public class Instance {

    private static MinecraftCpas instance;

    /*
    This class really shouldn't be nessesary but the API I use for mostly everything Spigotwise
    is exceptionally stupid in it's getInstance class (returning SimplePlugin rather then MinecraftCpas)
    so this class basically is initialized on startup to hold the instance of the main class.
     */


    public Instance() {
        assert true;
    }

    public void setInstance(MinecraftCpas instance) {
        this.instance = instance;
    }
    public MinecraftCpas getInstance() {
        return instance;
    }

}
