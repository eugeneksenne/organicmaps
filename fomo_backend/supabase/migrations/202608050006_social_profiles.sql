alter table public.profiles
  add column if not exists bio text check (char_length(bio) <= 500),
  add column if not exists city_id text,
  add column if not exists website text check (char_length(website) <= 2048),
  add column if not exists moments_visibility text not null default 'public' check (moments_visibility in ('public', 'followers', 'private'));

create or replace view public.profile_statistics as
select p.id,
  (select count(*) from public.moments m where m.creator_id = p.id and m.deleted_at is null and m.moderation_state = 'approved') as moment_count,
  (select count(*) from public.user_follows f where f.followee_id = p.id) as follower_count,
  (select count(*) from public.user_follows f where f.follower_id = p.id) as following_count
from public.profiles p;

grant select on public.profile_statistics to authenticated;
