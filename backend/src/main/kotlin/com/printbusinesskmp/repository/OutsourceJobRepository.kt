package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.OutsourceJobsTable
import com.printbusinesskmp.models.OutsourceJob
import com.printbusinesskmp.models.OutsourceJobCreateRequest
import com.printbusinesskmp.models.OutsourceJobStatus
import com.printbusinesskmp.models.OutsourceJobUpdateRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class OutsourceJobRepository {

    suspend fun allJobs(): List<OutsourceJob> = dbQuery {
        OutsourceJobsTable.selectAll().map(::toJob)
    }

    suspend fun jobsByOrderId(orderId: String): List<OutsourceJob> = dbQuery {
        OutsourceJobsTable.selectAll()
            .where { OutsourceJobsTable.orderId eq orderId }
            .map(::toJob)
    }

    suspend fun jobById(id: String): OutsourceJob? = dbQuery {
        OutsourceJobsTable.selectAll()
            .where { OutsourceJobsTable.id eq id }
            .map(::toJob)
            .singleOrNull()
    }

    suspend fun addJob(request: OutsourceJobCreateRequest): OutsourceJob = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Instant.now()

        OutsourceJobsTable.insert {
            it[OutsourceJobsTable.id] = id
            it[orderId] = request.orderId
            it[partnerId] = request.partnerId
            it[description] = request.description.trim()
            it[costToYou] = request.costToYou
            it[status] = request.status.name
            it[createdAt] = now
            it[updatedAt] = now
        }

        OutsourceJobsTable.selectAll()
            .where { OutsourceJobsTable.id eq id }
            .map(::toJob)
            .single()
    }

    suspend fun updateJob(id: String, request: OutsourceJobUpdateRequest): OutsourceJob? = dbQuery {
        val changed = OutsourceJobsTable.update({ OutsourceJobsTable.id eq id }) {
            it[orderId] = request.orderId
            it[partnerId] = request.partnerId
            it[description] = request.description.trim()
            it[costToYou] = request.costToYou
            it[status] = request.status.name
            it[updatedAt] = Instant.now()
        }

        if (changed == 0) {
            null
        } else {
            OutsourceJobsTable.selectAll()
                .where { OutsourceJobsTable.id eq id }
                .map(::toJob)
                .singleOrNull()
        }
    }

    suspend fun deleteJob(id: String): Boolean = dbQuery {
        OutsourceJobsTable.deleteWhere { OutsourceJobsTable.id eq id } > 0
    }

    private fun toJob(row: ResultRow): OutsourceJob {
        return OutsourceJob(
            id = row[OutsourceJobsTable.id],
            orderId = row[OutsourceJobsTable.orderId],
            partnerId = row[OutsourceJobsTable.partnerId],
            description = row[OutsourceJobsTable.description],
            costToYou = row[OutsourceJobsTable.costToYou],
            status = OutsourceJobStatus.valueOf(row[OutsourceJobsTable.status]),
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(row[OutsourceJobsTable.createdAt].toEpochMilli()),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(row[OutsourceJobsTable.updatedAt].toEpochMilli())
        )
    }
}
