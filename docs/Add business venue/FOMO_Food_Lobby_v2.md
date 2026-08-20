# FOMO Food Lobby — Premium Spec v2
## "The Digital Storefront" — Enhanced

---

## What Changed and Why

Your v1 draft is well-organized but structurally identical to Uber Eats / Google Maps business pages: hero → info → menu → reviews → location, all in a flat vertical scroll. Three things separate a premium, "billion-dollar" experience from a template:

1. **Social proof as a first-class citizen, not a future feature.** FOMO's entire value prop is location-aware social discovery. Right now the food lobby has zero FOMO-native signal — no friends, no live crowd, no check-ins. That's the one thing Uber Eats and Google Maps structurally can't copy. It should ship in v1, not "Smart Features (Future)."
2. **Sticky tab navigation instead of pure vertical scroll.** A 10-section scroll works for a simple listing page but feels slow on a hero-driven, image-heavy app. Premium food/travel apps (Resy, Airbnb, Beli) use a persistent hero + sticky sub-nav so users jump straight to Menu or Reviews without re-scrolling past Gallery every time.
3. **Dark-forward visual language.** FOMO is a nightlife app. A bright white "Uber Eats" card system will feel like a tonal mismatch next to your event/nightlife lobbies. This spec assumes a dark base theme with vibrant accent color per venue category, consistent with the rest of FOMO.

---

## Revised Structure

```
Hero (parallax, video-capable)
↓
Sticky Tab Bar: Overview | Menu | Photos | Reviews | Info
↓
[Overview Tab]
  → Live Signal Strip (busy now, friends here, check-ins)
  → Business Summary
  → Featured Dishes
  → Today's Specials / Flash Deals
  → Top Reviews (preview, 2-3 only)
  → Map preview
↓
[Menu Tab] → Full Menu
[Photos Tab] → Gallery
[Reviews Tab] → Full Reviews
[Info Tab] → Hours, Contact, Payment, Amenities, Directions
↓
Persistent Bottom Action Bar
```

This turns 10 forced sections into 1 fast overview + 4 on-demand deep dives. Users decide in 10 seconds (your original goal) and only dig deeper if interested.

---

## 1. Hero Experience — Enhanced

**Same base as v1**, plus:

- **Parallax scroll** on the cover image (subtle depth, not gimmicky) — signals premium polish immediately.
- **Video-capable hero**: venues can upload a 6–10s looping clip instead of a static photo (kitchen action, plating, ambiance). Static image remains the default/fallback.
- **Live occupancy chip**, pulled from FOMO check-in data:
  - 🟢 Not Busy · 🟡 Getting Busy · 🔴 Packed
  - This is a signal Google Maps only estimates from location history — you can have it be *real*, from your own users.
- **Friends signal** (if any FOMO friends have checked in or reviewed recently):
  - "3 friends have been here" with small avatar stack
  - Tapping opens a quick list with their ratings/photos

**Overlay (kept from v1, reordered by importance):**
```
✓ Verified          ⭐ 4.8 (312)         $$
🟢 Not Busy · Closes 22:00 · 2.3 km

3 friends have been here  [avatar stack]

🍔 Burgers   🍟 Fast Food   🥤 Drinks
```

**CTAs unchanged:** View Menu (primary) / Get Directions (secondary)

---

## 2. Live Signal Strip — NEW

A single horizontal scroll strip directly under the hero, before Business Summary. This is the section that makes the lobby feel alive and social rather than static.

Cards (only show what's populated — never empty-state all of them):
- 👀 **X people viewing this now** (light social proof, same pattern as booking apps)
- 🔥 **Trending dish this week**: photo + name
- 📍 **X FOMO users checked in today**
- 🎉 **Linked event tonight** (if a nearby FOMO event references this venue — e.g. pre-party dinner spot)

This directly answers "why would I trust this place *right now*" — which none of Uber Eats, Google Maps, or a generic review site can answer, because they don't have your event/social graph.

---

## 3. Business Summary

Same as v1 — kept lightweight intentionally. Add:
- **Response rate / reply time badge** if owner is active: "Replies within 1 hour" — this is a trust signal that meaningfully drives conversion on marketplaces and costs nothing extra to compute from your Creator Studio data.

---

## 4. Featured Dishes

Same as v1. Enhancement:
- Tag badges on cards: 🔥 Trending · 🆕 New · 🌱 Vegan — pulled from the same tags used in Menu filters, so venues only tag once.

---

## 5. Full Menu

Same as v1 (sections, search, filters). Enhancement:
- **"Most ordered this week"** sort option, if you support in-app ordering — see Open Decision #1 below, this depends on whether Menu is browse-only or transactional.

---

## 6. Gallery

Same as v1. Enhancement:
- Split into **Business Photos** (owner-uploaded, curated) vs **Community Photos** (user-uploaded via check-ins/reviews) — clearly labeled tabs within Gallery. This keeps professional shots from being buried by phone snapshots, while still surfacing authentic social proof.

---

## 7. Today's Specials / Flash Deals

Merge these two into one section — in v1 they're nearly identical UX patterns (time-boxed offer + countdown) and having both back-to-back is redundant. One "Offers" section with two card types (Daily Special / Flash Deal) is cleaner.

Enhancement: Flash Deals get a **push notification hook** — users who've favorited the venue get notified when a flash deal goes live. This is a strong retention lever specific to your app and something Google Maps/Uber Eats can't replicate without a stored preference relationship.

---

## 8. Reviews

Same as v1 structure. Enhancement:
- **"Verified Visit" tag** on reviews from users who actually checked in via FOMO at that location (location-verified, not just a purchase receipt). This is a trust signal exclusive to your architecture and stronger than most review platforms.
- Photo reviews get a horizontal carousel at the top of the Reviews tab, above the list.

---

## 9. Business Information

Same as v1. No changes — this section is meant to be plain and scannable, and it already is.

---

## 10. Location

Same as v1, enhancement:
- **"Friends nearby now"** micro-signal if applicable, reusing the same social data from the Live Signal Strip — reinforces the social layer without adding new data plumbing.

---

## Bottom Action Bar

Unchanged from v1 — Get Directions primary, Call/WhatsApp secondary. Correct choice; don't overload it.

---

## Lobby Variants — Unchanged Logic, One Addition

Your tiering (Official / Claimed / FOMO-created) is the right model and creates a clean claim incentive. One addition:

- **Official (unclaimed) businesses** should still show the Live Signal Strip (busy now, friends here) if data exists — since that data comes from your users, not the business. Withholding it would weaken the product for users just because a business hasn't claimed yet. Reserve the *business-facing* tools (Menu Management, Flash Deals, Analytics) as the claim incentive — not user-facing social signal.

---

## Design Philosophy — Updated

The Food Lobby should feel **alive, social, and premium** — not just visual and conversion-focused. The differentiator isn't prettier photos than Uber Eats; it's that FOMO can show *your friends were just here* and *it's busy right now*, in real time, because of the check-in and social graph the rest of the app already has. Lean into that everywhere it's structurally free to do so.

Dark base theme, vibrant per-category accent colors, parallax hero, sticky tab nav — these bring the visual craft up to a premium bar. The social/live layer is what makes it defensibly *FOMO* rather than a reskinned delivery app.

---

## Open Decision Points

**1. Is the Menu browse-only or transactional?**
- A) Browse-only — Menu is informational, all conversion happens via Directions/Call/WhatsApp (matches current v1 CTAs)
- B) In-app ordering — requires cart, payment, order status, and a completely different Menu tab and bottom bar
- C) Reservation/booking only — no ordering, but table booking flow
This changes the Menu tab, bottom action bar, and whether "Most ordered" sorting is buildable.

**2. Does the Live Signal Strip launch in v1 or is it deferred?**
- A) Ship in v1 — requires check-in data to already exist and be reliable at launch
- B) Defer to v1.1 — ship the lobby without it, backfill once check-in volume is meaningful (avoids an empty/dead-looking strip on quiet nights)

**3. Video hero — in scope for launch?**
- A) Yes — venues can upload short looping video
- B) Static image only for v1, video as fast-follow (lower encoding/storage complexity at launch)
