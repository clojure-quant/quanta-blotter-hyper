



oms_events table                 │         │
│   stream_id | sequence | event_type   │         │
│   occurred_utc | payload_json  


Projection Layer         │  │       Dispatch Tracker            │
│   trade_states table (SQLite) │  │   dispatch_markers table          │
│   Derived read model          │  │   Idempotency key registry        │
│   37-column flat projection 



#### Append-Only Event Sourcing

OMS records every lifecycle fact as an immutable event in the `oms_events` SQLite table. No event is ever updated or deleted. The sequence of events per `stream_id` is the authoritative record of what happened.

```
Stream: EURUSD-LONG-20260519-001
───────────────────────────────────────────────────────────────
Seq 1  │ RiskDecisionEvaluated  │ Outcome=Approved
Seq 2  │ OrderSubmitted         │ Entry=1.0850, SL=1.0800
Seq 3  │ TradeEntered           │ Fill=1.0851, TP1=1.0900
Seq 4  │ StopMoved              │ New SL=1.0830
Seq 5  │ PartialExitTriggered   │ TP1 hit
Seq 6  │ TradeClosed            │ Exit=1.0920, NetR=+1.8
───────────────────────────────────────────────────────────────


### Core Responsibilities

| Responsibility | Description |
|---------------|-------------|
| **Dashboard Visualization** | Display live summary of open trades, P&L, risk exposure, system health |
| **Account Management** | Display account information, balances, linked broker connections |
| **Trade Monitoring** | Show real-time trade status, entries, stops, take-profit levels |
| **Lifecycle Visualization** | Render trade lifecycle stages and transitions clearly |
| **Trade History** | Paginated list of closed and cancelled trades with filtering |
| **Event Timeline** | Per-trade chronological event history for audit and review |
| **Notifications & Errors** | Surface OMS operational errors, dispatch failures, risk rejections |
| **Broker Connection Visibility** | Show broker connectivity status, MT4 gateway health |
| **Settings & Preferences** | User-configurable UI preferences (timezone, theme, notification rules) |
| **Live Update Consumption** | Connect to OMS update streams; refresh state on notifications |
| **API Authentication** | Handle JWT token lifecycle (acquire, refresh, attach to requests) |


#### Admin Controller — `GET /api/admin/ingestion-stats`

| Property | Value |
|----------|-------|
| Method | GET |
| Route | `/api/admin/ingestion-stats` |
| Purpose | Summary counts for event store, projections, dispatch markers |

**Response:**
```json
{
  "totalEventCount": 842,
  "totalProjectionCount": 61,
  "totalDispatchMarkerCount": 58,
  "asOfUtc": "2026-05-19T10:30:00Z"
}
```

Array of `TradeStateProjection`
```json
[
  {
    "streamId": "EURUSD-20260519-001",
    "lifecycleId": "lc-abc123",
    "symbol": "EURUSD",
    "status": "Open",
    "directionText": "Long",
    "lastEventType": "TradeEntered",
    "lastSequence": 3,
    "updatedUtc": "2026-05-19T09:05:00Z",
    "entryOrderType": "Market",
    "entryOrderPrice": 1.0850,
    "filledPrice": 1.0851,
    "initialSlPrice": 1.0800,
    "currentSlPrice": 1.0800,
    "tp1Price": 1.0900,
    "tp2Price": 1.0950,
    "tp3Price": 1.1000,
    "brokerOrderId": "MT4-10045",
    "sourceProtocol": "PineConnector",
    "netR": null,
    "remainingPct": 100.0
  }
]
```


---

#### Admin Controller — `GET /api/admin/dispatch-markers`

| Property | Value |
|----------|-------|
| Method | GET |
| Route | `/api/admin/dispatch-markers` |
| Purpose | Lists all dispatch tracking records |

**Response:** Array of `DispatchMarkerRecord`
```json
[
  {
    "idempotencyKey": "lc-abc123::ord-001",
    "lifecycleId": "lc-abc123",
    "orderId": "ord-001",
    "dispatchedAtUtc": "2026-05-19T09:00:00Z",
    "brokerOrderReference": "MT4-10045"
  }

  trading accounts.

**Request Parameters:** None

**Response:**
```json
{
  "accounts": [
    {
      "accountId": "acc-001",
      "displayName": "Main Prop Account",
      "brokerType": "MT4",
      "brokerAccountNumber": "12345678",
      "currency": "USD",
      "status": "Active",
      "linkedAt": "2026-01-01T00:00:00Z"
    }


     "trades": [
    {
      "tradeId": "EURUSD-20260519-001",
      "lifecycleId": "lc-abc123",
      "symbol": "EURUSD",
      "direction": "Long",
      "status": "Open",
      "entryPrice": 1.0851,
      "currentStopPrice": 1.0800,
      "takeProfit1": 1.0900,
      "openedAt": "2026-05-19T09:05:00Z",
      "updatedAt": "2026-05-19T10:00:00Z",
      "netR": null,
      "remainingPct": 100.0,
      "brokerRef": "MT4-10045"
    }

    {
  "tradeId": "EURUSD-20260519-001",
  "lifecycleId": "lc-abc123",
  "symbol": "EURUSD",
  "direction": "Long",
  "status": "Open",
  "sourceProtocol": "PineConnector",
  "entryOrderType": "Market",
  "requestedEntryPrice": 1.0850,
  "filledPrice": 1.0851,
  "initialStopPrice": 1.0800,
  "currentStopPrice": 1.0800,
  "takeProfits": [
    { "level": 1, "price": 1.0900, "percent": 33.3, "hit": false },
    { "level": 2, "price": 1.0950, "percent": 33.3, "hit": false },
    { "level": 3, "price": 1.1000, "percent": 33.4, "hit": false }
  ],
  "quantity": 0.1,
  "remainingQuantity": 0.1,
  "remainingPct": 100.0,
  "netR": null,
  "openedAt": "2026-05-19T09:05:00Z",
  "updatedAt": "2026-05-19T10:00:00Z",
  "closedAt": null,
  "closeReason": null,
  "brokerRef": "MT4-10045",
  "brokerPositionId": null,
  "riskDecision": {
    "outcome": "Approved",
    "profileName": "Generic Prop Challenge",
    "violations": []
  },
  "events": [
    {
      "sequence": 1,
      "eventType": "RiskDecisionEvaluated",
      "occurredAt": "2026-05-19T09:00:00Z",
      "summary": "Risk approved — 0 violations"
    },
    {
      "sequence": 2,
      "eventType": "OrderSubmitted",
      "occurredAt": "2026-05-19T09:00:01Z",
      "summary": "Order submitted for EURUSD Long at 1.0850"
    },
    {
      "sequence": 3,
      "eventType": "TradeEntered",
      "occurredAt": "2026-05-19T09:05:00Z",
      "summary": "Trade entered at 1.0851"
    }
  ]
}


/api/me/activity`

Returns a recent activity feed of system events relevant to the user.

**Query Parameters:** `limit` (default 50), `fromSequence` (for incremental loading)

**Response:**
```json
{
  "items": [
    {
      "id": "evt-001",
      "type": "TradeOpened",
      "title": "Trade opened: EURUSD Long",
      "detail": "Entry at 1.0851, SL at 1.0800",
      "tradeId": "EURUSD-20260519-001",
      "occurredAt": "2026-05-19T09:05:00Z",
      "severity": "Info"
    },
    {
      "id": "evt-002",
      "type": "RiskRejection",
      "title": "Order rejected: GBPUSD Long",
      "detail": "Daily loss limit reached. Violations: [DailyLoss]",
      "tradeId": null,
      "occurredAt": "2026-05-19T08:45:00Z",
      "severity": "Warning"
    }
  ],
  "nextFromSequence": 843


  "notifications": [
    {
      "notificationId": "notif-001",
      "type": "DispatchFailed",
      "title": "Broker dispatch failed",
      "message": "Order for GBPUSD could not be dispatched. Manual review required.",
      "tradeId": "GBPUSD-20260519-002",
      "severity": "Error",
      "read": false,
      "createdAt": "2026-05-19T09:30:00Z"
    }

     Trade Summary DTO

```json
{
  "tradeId": "EURUSD-20260519-001",
  "lifecycleId": "lc-abc123",
  "symbol": "EURUSD",
  "direction": "Long",
  "status": "Open",
  "entryPrice": 1.0851,
  "currentStopPrice": 1.0800,
  "takeProfit1": 1.0900,
  "takeProfit2": 1.0950,
  "takeProfit3": 1.1000,
  "remainingPct": 100.0,
  "netR": null,
  "openedAt": "2026-05-19T09:05:00Z",
  "updatedAt": "2026-05-19T10:00:00Z",
  "brokerRef": "MT4-10045",
  "sourceProtocol": "PineConnector"
}
```

### Trade Detail DTO

```json
{
  "tradeId": "EURUSD-20260519-001",
  "lifecycleId": "lc-abc123",
  "symbol": "EURUSD",
  "direction": "Long",
  "status": "PartialExited",
  "entryOrderType": "Market",
  "requestedEntryPrice": 1.0850,
  "filledPrice": 1.0851,
  "intendedQuantity": 0.3,
  "filledQuantity": 0.3,
  "remainingQuantity": 0.2,
  "averageFillPrice": 1.0851,
  "initialStopPrice": 1.0800,
  "currentStopPrice": 1.0830,
  "takeProfits": [
    { "level": 1, "price": 1.0900, "percent": 33.3, "hit": true, "hitAt": "2026-05-19T11:00:00Z" },
    { "level": 2, "price": 1.0950, "percent": 33.3, "hit": false },
    { "level": 3, "price": 1.1000, "percent": 33.4, "hit": false }
  ],
  "remainingPct": 66.7,
  "netR": null,
  "mfeR": 1.2,
  "maeR": -0.2,
  "openedAt": "2026-05-19T09:05:00Z",
  "updatedAt": "2026-05-19T11:05:00Z",
  "closedAt": null,
  "closeReason": null,
  "brokerRef": "MT4-10045",
  "brokerPositionId": null,
  "sourceProtocol": "PineConnector",
  "riskDecision": {
    "outcome": "Approved",
    "profileId": "prof-001",
    "profileName": "Generic Prop Challenge",
    "riskMode": "Enforced",
    "violations": []
  }
}
```

### Event History Entry

```json
{
  "sequence": 4,
  "eventType": "StopMoved",
  "occurredAt": "2026-05-19T10:30:00Z",
  "sourceProtocol": "PineConnector",
  "correlationId": "sha256-def456...",
  "summary": "Stop moved from 1.0800 to 1.0830",
  "detail": {
    "previousStopPrice": 1.0800,
    "newStopPrice": 1.0830,
    "reason": "TP1 hit — stop moved to entry"
  }
}
```

### Account Summary DTO

```json
{
  "accountId": "acc-001",
  "displayName": "Main Prop Account",
  "brokerType": "MT4",
  "brokerAccountNumber": "12345678",
  "currency": "USD",
  "status": "Active",
  "stats": {
    "openTradeCount": 3,
    "totalOpenRiskPct": 2.7,
    "closedTodayCount": 1,
    "closedTodayNetR": 1.4
  }
}
```

### Broker Connection Status DTO

```json
{
  "accountId": "acc-001",
  "brokerType": "MT4",
  "status": "Connected",
  "lastHeartbeatAt": "2026-05-19T10:29:55Z",
  "latencyMs": 12,
  "gatewayVersion": "1.0.0",
  "capabilities": {
    "marketOrders": true,
    "pendingOrders": true,
    "pendingModify": true,
    "pendingCancel": true
  },
  "notes": null
}
```

### Reconciliation Status DTO

```json
{
  "runId": "recon-20260519-001",
  "triggeredAt": "2026-05-19T10:00:00Z",
  "durationMs": 234,
  "scope": "KnownOnly",
  "dryRun": false,
  "snapshotStatus": "Success",
  "candidatesScanned": 8,
  "correctedCount": 1,
  "alreadyConsistentCount": 7,
  "failedCount": 0,
  "entries": [
    {
      "lifecycleId": "lc-xyz999",
      "action": "OrderFilled",
      "outcome": "Corrected",
      "brokerRef": "MT4-10044",
      "detail": "Fill confirmed at 1.3200, 0.1 lots"
    }
  ]
}
```

### Notification DTO

```json
{
  "notificationId": "notif-001",
  "type": "DispatchFailed",
  "title": "Broker dispatch failed",
  "message": "Order for GBPUSD Long could not be dispatched to MT4 gateway. Manual review required.",
  "severity": "Error",
  "tradeId": "GBPUSD-20260519-002",
  "lifecycleId": "lc-xyz999",
  "read": false,
  "createdAt": "2026-05-19T09:30:00Z",
  "expiresAt": null
}
```

### API Error Response

```json
{
  "error": {
    "code": "TRADE_NOT_FOUND",
    "message": "Trade with ID 'EURUSD-20260519-999' was not found.",
    "details": {
      "requestedId": "EURUSD-20260519-999"
    },
    "occurredAt": "2026-05-19T10:30:00Z",
    "requestId": "req-xyz789"
  }
}
```

### Live Update Event Payloads (SignalR)

**TradeUpdated:**
```json
{
  "event": "TradeUpdated",
  "tradeId": "EURUSD-20260519-001",
  "status": "PartialExited",
  "lastEventType": "PartialExitTriggered",
  "updatedAt": "2026-05-19T11:00:00Z"
}
```

**BrokerStatusChanged:**
```json
{
  "event": "BrokerStatusChanged",
  "brokerType": "MT4",
  "accountId": "acc-001",
  "status": "Disconnected",
  "previousStatus": "Connected",
  "occurredAt": "2026-05-19T10:45:00Z"
}
```

**RiskDecision:**
```json
{
  "event": "RiskDecision",
  "lifecycleId": "lc-new001",
  "symbol": "GBPUSD",
  "outcome": "Rejected",
  "violations": [
    {
      "category": "DailyLoss",
      "severity": "Hard",
      "message": "Daily loss limit reached: -2.1% vs limit -2.0%"
    }
  ],
  "occurredAt": "2026-05-19T10:50:00Z"
}
```
