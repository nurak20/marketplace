package com.oeurn_nurak.marketplace.dto.request;

import com.oeurn_nurak.marketplace.entity.enums.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReactToPostRequest {
    @NotNull(message = "Reaction type is required")
    private ReactionType reactionType;
}
