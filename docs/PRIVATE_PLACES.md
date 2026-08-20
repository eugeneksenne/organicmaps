# Private places

This project keeps private/community places separate from OpenStreetMap data. Private places are never uploaded to OSM and must not be represented using fake OSM IDs.

## Source identity

Every private place has a stable Supabase UUID and is marked with `source = private`. OSM features retain their normal `node/*`, `way/*`, or `relation/*` identifiers.

## Export contract

The map generator consumes a sanitized JSON export, not a Supabase service-role connection. See `data/private_places.schema.json`.

```text
Supabase (server-side export job)
  -> private_places.json
  -> map generator import stage
  -> private FeatureBuilder objects
  -> country.mwm.tmp
  -> final country .mwm
```

The export must contain only records approved for the target map release. Never put Supabase service keys in the app, generator arguments, MWM files, or source control.

## Visibility model

- Records included in an MWM are visible to every user who receives that MWM.
- Account-specific or confidential records must remain an online Supabase overlay.
- MWM records are snapshots; changing a record requires a new map generation or an online update.

## Attribution and editing

Private features should display the private/community source, for example `Provided by My Community`. They must not expose OSM edit actions or be sent through the OSM editor.

The app must continue to show OpenStreetMap attribution and comply with the ODbL for the OSM-derived portion. Private data can have a separate licence.

## Suggested Supabase table

```sql
create table public.private_places (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  category text not null,
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  address text,
  phone text,
  website text,
  photos jsonb not null default '[]'::jsonb,
  social_links jsonb not null default '{}'::jsonb,
  status text not null default 'approved',
  map_release boolean not null default true,
  updated_at timestamptz not null default now()
);

alter table public.private_places enable row level security;

create policy "approved places are readable"
on public.private_places for select
using (status = 'approved' and map_release = true);
```

The export job should apply the same approval and release filters before producing the JSON consumed by map generation.
