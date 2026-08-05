# Open-source FOMO service stack

No paid API is required for the backend foundation in this repository.

| Capability | Self-hosted component | License/role |
| --- | --- | --- |
| Auth, Postgres, Storage, Realtime | Supabase | Open-source stack, self-hostable |
| Database | PostgreSQL | PostgreSQL License |
| WebRTC SFU, rooms, egress | LiveKit | Apache-2.0, self-hostable |
| Ephemeral realtime gateway | Socket.IO | MIT |
| Cache and Socket.IO fanout | Valkey | BSD-3-Clause |
| Object storage | MinIO | AGPLv3 |
| Android-compatible push option | ntfy / UnifiedPush gateway | Open source, self-hosted |

`LiveKit Cloud`, hosted Supabase, FCM, APNs, Mux, and other managed providers are not runtime requirements in this codebase. They should remain optional adapters only. If iOS background push is required, APNs is an unavoidable platform service; it is not used by the Android/VPS implementation here.

All provider keys, deploy credentials, TURN secrets, and service-role keys must be stored outside Git regardless of whether the service is self-hosted.
