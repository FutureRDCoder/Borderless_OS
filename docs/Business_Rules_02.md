# BorderlessOS — Business Rules 02: Amounts, Currencies, and Indicative FX Quotes

**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Project phase:** Day 1 — Step 4: Business Rules & Exception Handling  
**Specification ID:** BR-02  
**Status:** Approved  
**Scope:** Five-day educational MVP

## 1. Purpose and scope

BR-02 defines amount validation, supported currencies, indicative foreign-exchange (FX) quotes, external-provider fallback, quote expiry, calculation precision, rounding, and quote presentation for the BorderlessOS prototype.

BorderlessOS is an educational prototype, not a licensed financial product or certified compliance system. It must not move, custody, disburse, or settle real funds. FX quotes are estimates only and do not guarantee an exchange rate or recipient payout.

## 2. Approved product decisions

- **Amount limits:** Configurable minimum and maximum per currency.
- **FX source:** External FX API with deterministic mock fallback.
- **Quote expiry:** Five minutes.
- **Fees:** No fees in the MVP.
- **Rounding:** Destination currency's configured minor-unit precision, using decimal arithmetic.
- **Rate direction:** Destination-currency units per one unit of source currency.
- **Rounding mode:** `HALF_UP` is proposed for consistent implementation.
- **Expiry boundary:** A quote is expired when server time is greater than or equal to `expiresAt`.

## 3. Business rules

### BR-02.1 — Amount validation

The send amount must be greater than zero and within the configured minimum and maximum for the source currency. Reject missing, malformed, non-finite, zero, negative, and out-of-range amounts. Apply validation on the backend.

### BR-02.2 — Currency validation

The source and destination currencies must be supported by the application. The destination currency must also be compatible with the selected beneficiary under BR-01. Unsupported currency pairs must not produce a usable quote.

### BR-02.3 — External FX API with mock fallback

Attempt to retrieve a rate from the configured external FX provider. If the provider fails, times out, returns invalid data, or does not support the requested pair, use a deterministic mock rate only if one is configured for that pair. The quote must clearly identify whether its rate came from an external source or a mock.

If the external provider fails and no configured mock rate exists for the pair, return a controlled error. Never invent a rate or represent a mock rate as external data.

### BR-02.4 — Indicative quote only

A quote represents an estimate for the requested amount and currency pair. It is not a guaranteed rate, an executed exchange, or a promise that a recipient will receive the displayed amount.

### BR-02.5 — Quote calculation

Define the rate as destination-currency units per one unit of source currency.

`estimatedDestinationAmount = sendAmount × exchangeRate`

Use Java `BigDecimal` for monetary calculations. Do not use `float` or `double` for financial amount calculations.

### BR-02.6 — Currency-aware rounding

Round the calculated destination amount to the configured minor-unit precision of the destination currency. Use an explicit rounding mode; `HALF_UP` is proposed for the MVP and must be applied consistently.

### BR-02.7 — Quote expiry

Each quote expires five minutes after its issue timestamp. An expired quote cannot be treated as valid for a new transfer submission. The backend must check expiry; a frontend countdown alone is insufficient.

A quote is expired when the server's current time is greater than or equal to `expiresAt`.

### BR-02.8 — Quote binding

Bind each quote to its source currency, destination currency, send amount, rate, estimated destination amount, issue time, expiry time, and rate source. A quote must not be reused with a different amount or currency pair. Quote values and timestamps are generated and validated by the server; client-supplied edits are not authoritative.

### BR-02.9 — No fees in the MVP

No fee is added to or deducted from the transfer amount. The interface must not display a fabricated fee. The estimated destination amount is calculated directly from the send amount and quoted rate.

### BR-02.10 — Transparent quote presentation

Display the send amount, source and destination currencies, FX rate, estimated destination amount, quote source, and expiry information. Clearly label mock-derived rates and estimated amounts.

## 4. Quote data model

The proposed quote response contains:

| Field | Purpose |
|---|---|
| `quoteId` | Unique identifier for the quote. |
| `sourceCurrency` | Currency the customer sends. |
| `destinationCurrency` | Currency used for the estimate. |
| `sendAmount` | Requested source amount. |
| `exchangeRate` | Destination-currency units per source-currency unit. |
| `estimatedDestinationAmount` | Converted amount after rounding. |
| `rateSource` | `EXTERNAL` or `MOCK`. |
| `issuedAt` | Server-generated issue timestamp. |
| `expiresAt` | Server-generated expiry timestamp. |

These are proposed fields for the implementation; the exact external FX provider and response schema remain to be selected.

## 5. Validation rules and edge cases

| Scenario | Expected behaviour |
|---|---|
| Amount is zero or negative | Reject the request. |
| Amount is below the configured minimum | Reject with a clear validation error. |
| Amount exceeds the configured maximum | Reject with a clear validation error. |
| Amount has malformed numeric input | Reject without creating a quote. |
| Amount has excessive decimal precision for the source currency | Reject rather than silently changing the amount. |
| Source or destination currency is unsupported | Reject the quote request. |
| Beneficiary currency conflicts with destination currency | Reject the quote or transfer request. |
| External FX API times out or is unavailable | Use a configured mock rate and label the quote `MOCK`; otherwise return a controlled error. |
| External provider returns malformed, non-positive, or unusable rate data | Do not use that response; attempt configured mock fallback. |
| No external rate and no mock rate exist for the pair | Return a controlled error; do not invent a rate. |
| Quote is five minutes old or older | Treat it as expired. |
| Customer changes amount or currency pair | Generate a new quote. |
| Customer submits a quote belonging to another amount or pair | Reject the mismatch. |
| Conversion produces more decimal places than the destination currency allows | Round using the configured minor-unit precision and rounding mode. |
| Destination currency has precision different from two decimal places | Apply that currency's configured precision. |
| Browser countdown differs from server time | Use server timestamps and server-side expiry validation. |
| Quote is requested repeatedly | Each response must have coherent quote identity and timestamps; retries must not cause unintended transfer creation. |

## 6. Illustrative calculation

The following values are fictional and demonstrate the calculation only.

| Item | Example |
|---|---:|
| Send amount | 100.00 USD |
| Mock rate | 0.92 EUR per USD |
| Estimated destination amount | 92.00 EUR |
| Fee | None in MVP |
| Quote validity | Five minutes |

Calculation:

`100.00 USD × 0.92 EUR/USD = 92.00 EUR`

This is not a live rate, a guaranteed payout, or an actual financial transaction.

## 7. Proposed Spring Boot implementation approach

- **Configuration:** Maintain per-currency minimum/maximum limits, supported currency pairs, and currency minor-unit precision.
- **FX provider abstraction:** Define an interface such as `FxRateProvider`, with an external API implementation and a deterministic mock implementation.
- **Fallback service:** Handle timeout, provider failure, unsupported pairs, and invalid responses without silently fabricating external data.
- **Quote service:** Validate the request, obtain a rate, calculate the estimate using `BigDecimal`, and issue a quote with server timestamps.
- **Quote validation:** Before accepting a transfer request, verify quote expiry and ensure the amount and currency pair match the quote.
- **Testing:** Use fixed mock rates and a controllable clock for deterministic expiry, fallback, boundary, and rounding tests.

The external FX provider must be selected and configured during implementation. Its API, credentials, rate limits, and data freshness are not specified by this business-rule decision and must not be assumed.

## 8. Acceptance criteria

- [ ] Per-currency minimum and maximum send amounts are configurable.
- [ ] Invalid, zero, negative, and out-of-range amounts are rejected server-side.
- [ ] Only configured currency pairs can receive quotes.
- [ ] The external FX provider is attempted, with deterministic mock fallback where configured.
- [ ] Every quote identifies its rate source and has server-generated issue and expiry timestamps.
- [ ] Quotes expire after five minutes and cannot be used after expiry.
- [ ] Quote values are bound to the requested amount and currency pair.
- [ ] Monetary calculations use `BigDecimal` and explicit currency-aware rounding.
- [ ] No fees are charged or displayed in the MVP.
- [ ] UI labels clearly identify estimated amounts and mock rates.
- [ ] No real funds are moved, held, disbursed, or settled.

## 9. Out of scope and safety boundaries

- Guaranteed FX rates or guaranteed recipient payouts.
- Actual currency conversion or execution of an exchange.
- Real fund movement, custody, disbursement, or settlement.
- Inventing a rate when neither the external provider nor a configured mock rate can provide one.
- Treating mock rates as externally sourced data.
- Claiming that the prototype establishes real-world remittance eligibility or certifies compliance.

## 10. Approval record

BR-02 was reviewed and approved by the user before proceeding to BR-03.

**Next step:** BR-03 — Transfer submission and lifecycle. Work on BR-03 should begin only after this BR-02 approval has been recorded.
