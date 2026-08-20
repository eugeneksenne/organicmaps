# FOMO Feed

Clients call `feed_page(kind)` with a user JWT. Ranked rows come from `feed_items`
(written by `vps/src/feed-worker.js`). If the worker has not run, `feed_page`
falls back to approved Moments.

## Ripple engine

See `ripple.md`. `set_moment_reaction` records like / ripple / save. Momentum
bands (quiet → viral) are computed in `refresh_moment_momentum` and rolled up
to venue / event / city / creator by `refresh_ripple_graph`.

Clients never write `feed_items.score` or any `*_momentum.band`.

Trust weight uses verification, account age, and a reduced weight on a
creator's own Moment. Low-trust accounts still ripple but contribute less,
and are rate-limited more tightly.

## Privacy

`feed_page` returns invitation state (active / ended / venue_closed) only.
It does not return Who's Here or live venue presence. Ripple snapshots
never include those fields either.

## Not done

Camera publish/moderation, media CDN, LiveKit replay pipeline, creator
analytics dashboard, geofenced Who's Here prompts, Club Lobby heat UI,
and device-attestation anti-abuse.
