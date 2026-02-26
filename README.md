# ytt-pos

Production-oriented Android POS scaffold using Kotlin + Jetpack Compose and a modular architecture.

## Modules
- `app`: Compose UI, Hilt application wiring, navigation
- `domain`: pure Kotlin contracts and models
- `data`: Room, DataStore, repositories, and WorkManager sync
- `reporting`: reporting boundary module
- `hardware-printer-star`: printer integration boundary
- `hardware-cashdrawer`: cash drawer integration boundary
- `hardware-payments`: payment integration aggregator
  - `hardware-payments:paypal`
  - `hardware-payments:mock`
