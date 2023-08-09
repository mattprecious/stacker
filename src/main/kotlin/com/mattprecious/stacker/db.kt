package com.mattprecious.stacker

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mattprecious.stacker.db.RepoDatabase

inline fun withDatabase(
	path: String,
	crossinline block: (db: RepoDatabase) -> Unit,
) {
	JdbcSqliteDriver("jdbc:sqlite:$path").use { driver ->
		migrateIfNeeded(driver)
		block(
			RepoDatabase(
				driver = driver,
			),
		)
	}
}

private const val versionPragma = "user_version"

fun migrateIfNeeded(driver: JdbcSqliteDriver) {
	val oldVersion =
		driver.executeQuery(
			identifier = null,
			sql = "PRAGMA $versionPragma",
			parameters = 0,
			mapper = { cursor ->
				QueryResult.Value(
					if (cursor.next().value) {
						cursor.getLong(0)
					} else {
						null
					},
				)
			},
		).value ?: 0L

	val newVersion = RepoDatabase.Schema.version

	if (oldVersion == 0L) {
		RepoDatabase.Schema.create(driver)
		driver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
	} else if (oldVersion < newVersion) {
		RepoDatabase.Schema.migrate(driver, oldVersion, newVersion)
		driver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
	}
}
