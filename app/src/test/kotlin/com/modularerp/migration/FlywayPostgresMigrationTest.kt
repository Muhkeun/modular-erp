package com.modularerp.migration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection

@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgresMigrationTest {

    @Test
    fun `flyway migrations keep generated document numbers unique per tenant`() {
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        postgres.createConnection("").use { connection ->
            DOCUMENT_TABLES.forEach { table ->
                val uniqueConstraints = uniqueConstraints(connection, table)
                assertThat(uniqueConstraints)
                    .contains(listOf("tenant_id", "document_no"))
                    .doesNotContain(listOf("document_no"))
            }
        }
    }

    private fun uniqueConstraints(connection: Connection, tableName: String): List<List<String>> {
        val sql = """
            SELECT tc.constraint_name, kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_schema = kcu.table_schema
             AND tc.table_name = kcu.table_name
            WHERE tc.table_schema = 'public'
              AND tc.table_name = ?
              AND tc.constraint_type = 'UNIQUE'
            ORDER BY tc.constraint_name, kcu.ordinal_position
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, tableName)
            statement.executeQuery().use { rs ->
                val grouped = linkedMapOf<String, MutableList<String>>()
                while (rs.next()) {
                    val constraintName = rs.getString("constraint_name")
                    val columnName = rs.getString("column_name")
                    grouped.getOrPut(constraintName) { mutableListOf() }.add(columnName)
                }
                return grouped.values.map { it.toList() }
            }
        }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        private val DOCUMENT_TABLES = listOf(
            "purchase_requests",
            "purchase_orders",
            "rfqs",
            "sales_orders",
            "goods_receipts",
            "goods_issues",
            "work_orders",
            "journal_entries",
            "quality_inspections",
            "contracts",
        )
    }
}
