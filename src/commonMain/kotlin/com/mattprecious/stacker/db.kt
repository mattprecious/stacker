package com.mattprecious.stacker

import app.cash.sqldelight.db.use
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.mattprecious.stacker.db.Lock
import com.mattprecious.stacker.db.RepoDatabase
import com.mattprecious.stacker.db.jsonAdapter
import okio.Path

suspend inline fun withDatabase(path: Path, crossinline block: suspend (db: RepoDatabase) -> Unit) {
  NativeSqliteDriver(
      schema = RepoDatabase.Schema,
      name = path.name,
      onConfiguration = { config ->
        config.copy(
          extendedConfig =
            DatabaseConfiguration.Extended(
              foreignKeyConstraints = true,
              basePath = path.parent.toString(),
            )
        )
      },
    )
    .use { driver ->
      block(
        RepoDatabase(driver = driver, lockAdapter = Lock.Adapter(operationAdapter = jsonAdapter()))
      )
    }
}
