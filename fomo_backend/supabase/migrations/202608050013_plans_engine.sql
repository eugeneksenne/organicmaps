-- Plan lifecycle is event-sourced for live coordination and the post-night memory timeline.
alter table public.tonight_plans add column if not exists mode text not null default 'solo' check (mode in ('solo','duo','group'));
alter table public.tonight_plans add column if not exists phase text not null default 'planning' check (phase in ('draft','planning','waiting','countdown','active','ride','completed'));
alter table public.tonight_plans add column if not exists budget_cents integer check (budget_cents >= 0);
create table public.plan_templates (id uuid primary key default gen_random_uuid(), owner_id uuid references public.profiles(id) on delete cascade, title text not null, mode text not null check(mode in ('solo','duo','group')), public boolean not null default false, created_at timestamptz not null default now());
create table public.plan_activity (id uuid primary key default gen_random_uuid(), plan_id uuid not null references public.tonight_plans(id) on delete cascade, actor_id uuid references public.profiles(id) on delete set null, kind text not null check(kind in ('created','invited','accepted','stop_added','stop_changed','arrived','ride_started','completed')), payload jsonb not null default '{}', created_at timestamptz not null default now());
create index plan_activity_sync_idx on public.plan_activity(plan_id, created_at);
alter table public.plan_templates enable row level security; alter table public.plan_activity enable row level security;
create policy "public or owned templates readable" on public.plan_templates for select to authenticated using(public or owner_id=auth.uid());
create policy "owners manage templates" on public.plan_templates for all to authenticated using(owner_id=auth.uid()) with check(owner_id=auth.uid());
create policy "plan members read activity" on public.plan_activity for select to authenticated using(exists(select 1 from public.tonight_plans p where p.id=plan_id and p.owner_id=auth.uid()) or exists(select 1 from public.tonight_plan_members m where m.plan_id=plan_activity.plan_id and m.user_id=auth.uid()));
