package masterthesis.solver.config

object ConfigProvider {
    lateinit var config: ProfileConfig
        private set

    fun loadConfig() {
        config = ConfigLoader().loadConfig()
    }

    fun setConfig(config: ProfileConfig) {
        this.config = config
    }
}