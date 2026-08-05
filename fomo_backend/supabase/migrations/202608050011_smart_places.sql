create table public.smart_place_guides (id uuid primary key default gen_random_uuid(), city_id text not null, title text not null, context text not null, mood text not null, venue_count integer not null default 0, updated_at timestamptz not null default now(), active boolean not null default true);
create table public.smart_place_recommendations (guide_id uuid not null references public.smart_place_guides(id) on delete cascade, venue_id uuid not null references public.city_explore_venues(id) on delete cascade, reason text not null, rank smallint not null, primary key(guide_id, venue_id));
alter table public.smart_place_guides enable row level security; alter table public.smart_place_recommendations enable row level security;
create policy "active smart guides readable" on public.smart_place_guides for select to anon, authenticated using (active);
create policy "smart recommendations readable" on public.smart_place_recommendations for select to anon, authenticated using (true);
-- Curated recommendations are explainable: every scored venue carries a human-readable reason.
create or replace function public.smart_places_tonight(p_city_id text, p_mood text default null)
returns table (venue_id uuid, venue_name text, suburb text, rating numeric, reason text, guide_title text, open_now boolean)
language sql stable security invoker set search_path=public as $$
 select v.id, v.name, v.suburb, v.rating, r.reason, g.title,
  (v.is_24_hours or (v.opens_at is not null and v.closes_at is not null and case when v.closes_at > v.opens_at then localtime >= v.opens_at and localtime < v.closes_at else localtime >= v.opens_at or localtime < v.closes_at end))
 from public.smart_place_guides g join public.smart_place_recommendations r on r.guide_id=g.id join public.city_explore_venues v on v.id=r.venue_id
 where g.city_id=p_city_id and g.active and v.safety_state='approved' and (p_mood is null or g.mood=p_mood)
 order by r.rank asc;
$$;
grant execute on function public.smart_places_tonight(text,text) to anon, authenticated;
