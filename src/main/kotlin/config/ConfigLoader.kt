package masterthesis.config

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

class ConfigLoader {

    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val mapper = YAMLMapper.builder()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        .build()
        .registerKotlinModule()

    fun loadConfig(): ProfileConfig {
        val configFile = Paths.get("./src/main/resources/config.yaml")
        val rootConfig = Files.newBufferedReader(configFile).use { reader ->
            mapper.readValue(reader, RootConfig::class.java)
        }

        val profileConfig = rootConfig.profiles[rootConfig.activeProfile]
            ?: throw IllegalArgumentException("Profile '${rootConfig.activeProfile}' not found in configuration.")

        logger.info("Loaded profile: $profileConfig")
        return profileConfig
    }
}