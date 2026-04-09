# Payment Created Event Contract

## Overview

- Topic: `payment.created`
- Producer: `paymentservice`
- Consumer: `orderservice`
- Event kind: the topic name defines the event type; there is no separate `eventType` field in the payload
- Delivery semantics: at-least-once

## Payload

`payment.created` messages use the `PaymentCreatedEvent` JSON payload.

```json
{
  "eventId": "2f7c62f5-5e51-4688-b5d8-7dd7fc03d3d8",
  "id": "67e47f9b3f8a4b12c8f6ab21",
  "orderId": 101,
  "userId": 202,
  "status": "SUCCESS",
  "timestamp": "2026-03-27T10:15:30Z",
  "paymentAmount": 49.99
}
```

## Field Semantics

- `eventId`: unique event identifier. Consumers should use it for idempotent processing.
- `id`: payment identifier in MongoDB hex string form.
- `orderId`: order identifier associated with the payment.
- `userId`: user identifier associated with the payment.
- `status`: resulting payment status. Allowed values: `SUCCESS`, `FAILED`.
- `timestamp`: payment creation timestamp in ISO-8601 UTC format.
- `paymentAmount`: decimal payment amount.

## Compatibility Rules

- New required fields must be coordinated with `orderservice` before release.
- Existing field names and meanings are part of the contract and must remain backward compatible.
- Producer-side changes to topic name or payload shape require updating the shared schema and contract tests.
