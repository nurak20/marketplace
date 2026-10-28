package com.oeurn_nurak.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result of a profile update. {@code token} is only present when the email
 * changed, because the JWT subject is the account email.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileUpdateResponse(AccountResponse account, String token) {
}
