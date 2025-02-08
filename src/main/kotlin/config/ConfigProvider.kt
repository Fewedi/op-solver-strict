package masterthesis.config

object ConfigProvider {
    lateinit var config: ProfileConfig
        private set

    fun loadConfig() {
        config = ConfigLoader().loadConfig()
    }

    fun setConfig(config: ProfileConfig) {
        ConfigProvider.config = config
    }
}