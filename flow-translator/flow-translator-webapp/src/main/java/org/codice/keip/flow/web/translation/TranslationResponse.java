package org.codice.keip.flow.web.translation;

import org.codice.keip.flow.web.error.ApiError;

record TranslationResponse(String data, ApiError<TranslationErrorDetail> error) {}
