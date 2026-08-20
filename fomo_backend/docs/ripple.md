# FOMO Ripple Engine

A Like says "I like this." A Ripple says **"more people should experience this."**

Clients submit a tap. PostgreSQL records the Ripple, scores momentum, and
returns a public band. Clients never calculate momentum, velocity, decay, or
trust.

## Write path

`set_moment_reaction(target, p_reaction, p_on)` is the durable API.
`toggle_moment_reaction` flips and then calls it.

One Ripple per account per Moment (`moment_reactions` primary key). Removing
the Ripple is allowed. Offline clients store the desired `p_on` and replay
the same call.

Lifecycle: auth → visibility → duplicate/idempotent write → rate limit →
trust weight → moment score → venue/event/city/creator rollup.

## Public snapshot

`ripple_snapshot(kind, id)` and the `momentum` object on the write RPC return:

`kind`, `id`, `band`, `ripple_count`, `tonight_count`, `like_count`,
`save_count`, `comment_count`, `ripple_hour`, `updated_at`

They never return live location, Who's Here, Moment Invitation state, trust
score, or raw velocity.

Bands (same words on Moments, venues, events, cities, creators):

`quiet` → `active` → `heating` → `hot` → `viral`

## Scoring (server only)

- Trust: verified 1.0, account older than 7 days 0.8, new 0.4, own Moment 0.2
- Velocity: 20-minute, 1-hour, and tonight counts (Africa/Johannesburg)
- Live viewers add a small boost while `live_broadcasts.state = 'live'`;
  when Live ends the same Moment keeps its Ripples as the replay
- Decay: `exp(-hours_since_last_ripple / 12)`
- Low-trust accounts are capped at 10 new Ripples per hour; everyone is
  capped at 40 reaction writes per minute

`decay_ripple_momentum` is worker-only. It is not granted to `authenticated`.

## Cross-system reads

The ranking worker (`vps/src/feed-worker.js`) reads `decayed_score` and
does not invent bands. Discover / Smart Places / Flash Drops order by the
public band or city heat. Search can call `ripple_trending`.

## Privacy

Ripple is independent of Moment Invitations and Who's Here. Invitation
countdown is remaining invitation time, not venue hours. Who's Here stays
in Club Lobby and defaults to Private.

## Not done

Device attestation, emulator/device-farm detection, impossible-travel
checks, coordinated-attack graphs, creator analytics UI, Club Lobby heat
banners, push alerts, and realtime fanout of someone else's Ripple.
