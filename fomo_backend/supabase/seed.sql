-- Development seed data belongs here. Keep this file free of production data and secrets.
-- Auth users should be created through Supabase Auth, then matching profiles are inserted through
-- the profile-bootstrap RPC/trigger once that feature is implemented.

-- Sample approved venue snapshots around Johannesburg (Braamfontein / Sandton / Rosebank) so
-- the FOMO Camera can resolve the current venue for local development. These are coarse seed
-- coordinates; the trusted venue-ingestion worker is expected to overwrite them in production.
insert into public.venue_snapshots (venue_id, name, category, address, latitude, longitude, safety_state, live_now)
values
  ('venue_truth_jhb',       'Truth Nightclub',      'Nightclub',    'Sandton, Johannesburg',         -26.1076, 28.0567, 'approved', true),
  ('venue_cocoon_jhb',      'Cocoon Nightclub',     'Nightclub',    'Sandton, Johannesburg',         -26.1143, 28.0520, 'approved', true),
  ('venue_vault_jhb',       'The Vault',            'Lounge',       'Braamfontein, Johannesburg',    -26.1932, 28.0311, 'approved', true),
  ('venue_living_room_jhb', 'The Living Room',      'Rooftop',      'Maboneng, Johannesburg',        -26.2035, 28.0603, 'approved', false),
  ('venue_marabi_club_jhb', 'Marabi Club',          'Live Music',   'Maboneng, Johannesburg',        -26.2050, 28.0610, 'approved', false),
  ('venue_artivist_jhb',    'Artivist',             'Bar/Kitchen',  'Braamfontein, Johannesburg',    -26.1930, 28.0320, 'approved', false),
  ('venue_royale_jhb',      'The Royale',           'Cocktail Bar', 'Rosebank, Johannesburg',        -26.1451, 28.0422, 'approved', false),
  ('venue_rosebank_social', 'Rosebank Social',      'Bar',          'Rosebank, Johannesburg',        -26.1456, 28.0430, 'approved', false)
on conflict (venue_id) do update set
  name = excluded.name,
  category = excluded.category,
  address = excluded.address,
  latitude = excluded.latitude,
  longitude = excluded.longitude,
  live_now = excluded.live_now,
  safety_state = 'approved',
  updated_at = now();
