package com.cannatrace.data.local.mapper

import com.cannatrace.data.local.database.entities.BatchEntity
import com.cannatrace.data.local.database.entities.CorrectionNoteEntity
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.model.CorrectionNote

fun BatchEntity.toDomain(): Batch = Batch(
    id = id,
    batchNumber = batchNumber,
    strainName = strainName,
    status = BatchStatus.valueOf(status),
    weight = weight,
    thcContent = thcContent,
    cbdContent = cbdContent,
    createdAt = createdAt,
    updatedAt = updatedAt,
    ownerId = ownerId,
    locationId = locationId,
    parentBatchId = parentBatchId,
    isClosed = isClosed,
    hashChain = hashChain,
    notes = notes
)

fun Batch.toEntity(): BatchEntity = BatchEntity(
    id = id,
    batchNumber = batchNumber,
    strainName = strainName,
    status = status.name,
    weight = weight,
    thcContent = thcContent,
    cbdContent = cbdContent,
    createdAt = createdAt,
    updatedAt = updatedAt,
    ownerId = ownerId,
    locationId = locationId,
    parentBatchId = parentBatchId,
    isClosed = isClosed,
    hashChain = hashChain,
    notes = notes
)

fun CorrectionNoteEntity.toDomain(): CorrectionNote = CorrectionNote(
    id = id,
    batchId = batchId,
    operatorId = operatorId,
    justification = justification,
    createdAt = createdAt,
    correctionType = correctionType
)

fun CorrectionNote.toEntity(): CorrectionNoteEntity = CorrectionNoteEntity(
    id = id,
    batchId = batchId,
    operatorId = operatorId,
    justification = justification,
    createdAt = createdAt,
    correctionType = correctionType
)
