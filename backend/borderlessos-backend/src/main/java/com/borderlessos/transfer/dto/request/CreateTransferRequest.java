package com.borderlessos.transfer.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTransferRequest(
        @NotNull UUID beneficiaryId,
        @NotNull UUID quoteId
) {}