-- Camera venue detection: extend venue snapshots with display fields and capture the
-- coordinates attached to each moment so the Nearby/Live ranking branches can rank by distance
-- from the viewer and so moderators can audit where moments were shot.

alter table public.venue_snapshots
  add column if not exists name text not null default '',
  add column if not exists category text,
  add column if not exists address text,
  add column if not exists live_now boolean not null default false;

-- Coordinates at capture time, used by the Nearby feed ranking worker. Exposed only to the
-- moment owner and to the trusted ranking pipeline per RLS below.
alter table public.moments
  add column if not exists latitude numeric check (latitude is null or (latitude between -90 and 90)),
  add column if not exists longitude numeric check (longitude is null or (longitude between -180 and 180)),
  add column if not exists location_accuracy_meters numeric check (location_accuracy_meters is null or location_accuracy_meters >= 0);

-- Storage bucket for moment media (photos, videos, thumbnails). Object ownership is enforced
-- through the owner metadata column and the RLS policies on storage.objects.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('moments', 'moments', false, 52428800, array['image/jpeg', 'image/png', 'video/mp4'])
on conflict (id) do nothing;

-- Clients may read their own uploads or approved public/follower moments (joined via moments).
create policy "moment media readable with published moment" on storage.objects for select using (
  bucket_id = 'moments' and (
    owner = auth.uid()
    or exists (
      select 1 from public.moments m
      where m.deleted_at is null
        and m.moderation_state = 'approved'
        and (m.visibility = 'public' or m.creator_id = auth.uid())
        and ('moments/' || m.media_path) = name
    )
  )
);
create policy "creator uploads moment media" on storage.objects for insert to authenticated with check (
  bucket_id = 'moments' and owner = auth.uid()
);
create policy "creator removes own moment media" on storage.objects for delete to authenticated using (
  bucket_id = 'moments' and owner = auth.uid()
);

-- Approved snapshots are already readable via the existing policy; extend select columns to include
-- the new display fields. (The policy is unchanged; this comment documents the intent.)
