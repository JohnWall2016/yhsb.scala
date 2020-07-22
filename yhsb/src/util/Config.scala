package yhsb.util

import com.typesafe.config.ConfigFactory

object Config {
  def load(configPrefix: String) = {
    val factory = ConfigFactory.load(getClass.getClassLoader)
    if (factory.hasPath(configPrefix))
      factory.getConfig(configPrefix)
    else
      ConfigFactory.empty
  }
}