package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.RequestImageUploadRemote
import ai.askdiverge.domain.model.message.incoming.part.RequestImageUpload

internal fun RequestImageUploadRemote.mapToDomain(): RequestImageUpload? =
    RequestImageUpload.create(
        partId = part_id,
        acceptedTypes = accepted_types,
        maxSizeBytes = max_size_bytes
    )
