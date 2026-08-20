# FOMO Feed + Ripple shared logic

`FeedLogic` formats Moments. `RippleLogic` formats momentum badges and the
offline Ripple queue. Neither file computes momentum, velocity, decay, trust,
or feed rank.

Server RPCs own scores:

- `set_moment_reaction` / `toggle_moment_reaction` record the tap
- `refresh_moment_momentum` scores the Moment
- `refresh_ripple_graph` rolls up venue, event, city, and creator bands
- `decay_ripple_momentum` is worker-only

Ripple, Moment Invitation, and Who's Here stay separate. Feed never reads
Who's Here. Ripple snapshots never include live location or invitation state.
