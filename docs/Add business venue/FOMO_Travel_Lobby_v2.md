# FOMO Travel Lobby — Premium Spec v2
## "Your Journey Starts Here" — Enhanced

---

## What Changed and Why

This draft already reflects the lessons from the Prep Lobby work — Request Booking (not "Book"), Verified Visit reviews, sticky nav, merged Offers/Flash Deals. Good baseline. Five real gaps remain:

1. **The tab structure doesn't fit 7 of your 14 supported categories.** "Accommodation" is meaningless for Car Rental, Boat Charter, Tour Operator, Tourist Attraction, Adventure Park, Museum, and Scenic Experience — none of these have rooms. Right now the spec is really "Hotel/Lodge Lobby" with a category list stretched over it. This needs a category-adaptive tab config, not a one-size-fits-all template.
2. **No Live Signal Strip.** Food and Prep both established this as the FOMO-native differentiator (friends who've been here, trending, recent activity). Travel dropped it entirely — inconsistent across lobby types and a missed opportunity where it matters most (people trust travel decisions on social proof more than almost any other category).
3. **No person-level trust tagging.** Prep's biggest lesson was that people book *people* (stylists), not just businesses, and tagging portfolio images by professional was the single highest-leverage addition. Travel has the same dynamic for guided experiences — "led by Thabo, 8 years guiding" builds exactly the same trust as "cut by John."
4. **Hardcoded "/night" pricing** doesn't work for Car Rental (per day), Boat Charter (per trip/hour), Museum (per ticket), Tour Operator (per person). Needs a flexible pricing unit.
5. **Naming collision**: "Featured Experiences" (plural, multiple cards) and "Featured Experience" (singular, Experience of the Month) sit right next to each other in Overview — easy to confuse when skimming a spec or building UI copy.

---

## 1. Category-Adaptive Tab Structure (Priority Fix)

Split your 14 categories into three structural groups. Each gets a different Accommodation/Experiences/Fleet configuration rather than forcing every business through the same two tabs.

**Group A — Lodging** (Hotel, Guest House, B&B, Lodge, Resort, Safari Lodge)
```
Overview | Experiences | Accommodation | Gallery | Reviews | Info
```
Unchanged from current draft — this group is what the spec was written for.

**Group B — Experience/Attraction** (Tour Operator, Tourist Attraction, Adventure Park, Museum, Scenic Experience, Boat Charter)
```
Overview | Experiences | Gallery | Reviews | Info
```
No Accommodation tab at all. "Rooms & Packages" in Overview is replaced by nothing — Experiences already carries the catalogue weight for this group.

**Group C — Transport** (Car Rental)
```
Overview | Fleet | Gallery | Reviews | Info
```
New **Fleet tab** replaces both Experiences and Accommodation:
```
Economy — From R450/day
SUV — From R850/day
Luxury — From R1,800/day
```
Each fleet card: photo, category, price/day, transmission, seats, Request Booking.

This is a config decision at the business-type level, not a per-business toggle — when a business owner selects their category at onboarding, the correct tab set follows automatically.

---

## 2. Flexible Pricing Unit

Replace hardcoded "/night" everywhere with a pricing unit field per listing type:

| Category | Unit |
|---|---|
| Hotel, Lodge, Resort, B&B | /night |
| Car Rental | /day |
| Boat Charter | /trip or /hour |
| Tour Operator, Museum, Attraction | /person |
| Adventure Park | /entry |

Same "From R___" display pattern from the current draft, just parameterized rather than fixed to lodging.

---

## 3. Live Signal Strip — NEW (Parity with Food & Prep)

Same simplified pattern established in the Prep Lobby decision — only ship metrics that will reliably have real data, no bare "viewing now" counters:

* 🔥 **Trending experience this month** — name
* ⭐ **Featured package or experience**
* 📍 **X FOMO users have stayed/visited** (aggregate, not live)
* 👯 **Friends signal**, if applicable: "2 friends have stayed here" with avatar stack — highest-trust signal available for travel decisions specifically, since people weight friend recommendations heavily for where-to-stay choices

Sits directly under the Hero, above Travel Highlights — same position as the other two lobbies for consistency.

---

## 4. Guide / Host Tagging on Experiences — NEW

Direct application of the Prep Lobby lesson. Each Experience card optionally tags who runs it:

```
🦁 Sunset Safari
3 Hours · From R950/person
Led by Thabo · 8 Years Guiding · ⭐ 4.9

[Request Experience]
```

Not every category needs this (a museum ticket doesn't have a "guide"), so it's an optional field on the Experience card schema — populate where a business has named guides/instructors/hosts, omit otherwise. Reviews should support the same tagging pattern used in Prep ("Verified Visit — Guided by Thabo") so individual guides build reputation the same way individual stylists do.

---

## 5. Naming Fix

Rename the two sections to remove ambiguity:
- "Featured Experiences" (multiple cards, top of Overview) → keep as-is
- "Featured Experience" (single Experience of the Month card) → rename to **"Experience of the Month"** throughout, matching what the content actually is

---

## 6. Hero — Minor Enhancement

Keep the current cinematic single-image hero — unlike Prep, Travel's differentiator is destination photography, not a person's portfolio, so a Portfolio-style card grid would be the wrong pattern here. One addition:
- **Video-capable hero** (drone footage, property walkthrough, safari clip), same optional upgrade path used in the Food Lobby — static image remains default/fallback.

---

## Everything Else — Unchanged, Confirmed Good

These elements are already well-designed and shouldn't change:
- Request Booking model (V1) with WhatsApp/Call/Website choice and pre-filled enquiry message
- Sticky nav pattern (adjusted per category group above)
- Verified Visit tagging on reviews, with Experience/Accommodation review filters
- Merged Offers section (Standing Promotions + Flash Deals)
- Nearby Attractions in the Location section — a nice travel-specific touch not present in Food or Prep, keep it
- Official / Claimed / FOMO-created tiering, consistent with Food and Prep
- Difficulty field on Experience cards — good addition for adventure-category businesses

---

## Design Philosophy — Confirmed

The stated philosophy (cinematic, aspirational, experience-driven rather than listing-driven) is correct and doesn't need revision. The gap wasn't in the vision — it was that the structure underneath was built for lodging businesses and then had 9 more categories layered on top without adjusting the frame. Fixing the tab/pricing structure to actually flex per category, and bringing Travel up to parity with Food/Prep on social signal and person-level trust tagging, closes that gap without changing the creative direction at all.

---

## Open Decision Points

**1. Category grouping — does Fleet (Car Rental) ship in V1, or launch Car Rental with a generic Experiences-style tab as a stopgap?**
- A) Build the dedicated Fleet tab for V1 — more correct, more build work
- B) Launch Car Rental temporarily under the Experience/Attraction tab set (Group B) — vehicles listed as "experiences," acceptable short-term mislabel, less build work, migrate to Fleet later

**2. Guide/Host tagging — required field or optional at business discretion?**
- A) Optional — businesses add it only if they have named guides (recommended, avoids forcing solo/small operators to invent staff structure they don't have)
- B) Required for Tour Operator and Safari Lodge categories specifically, optional elsewhere

**3. Live Signal Strip — same launch timing question as Prep Lobby: ship in V1, or defer until check-in/visit data has enough volume to avoid a sparse-looking strip?**
- A) Ship in V1
- B) Defer to v1.1, consistent with whatever was decided for Food/Prep
