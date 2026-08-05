-- Development-only curated venue facts for the Explore City RPC. Not user activity or popularity data.
insert into public.city_explore_venues (id, city_id, name, suburb, discovery_world, venue_kind, rating, latitude, longitude, opens_at, closes_at, experience_tags, verified, quality_rank, featured)
values
  ('a1000000-0000-4000-8000-000000000001', 'johannesburg', 'Konka', 'Rosebank', 'nightlife', 'Cocktail bar', 4.8, -26.1451, 28.0417, '17:00', '04:00', array['Live DJ', 'Dance floor'], true, 92, true),
  ('a1000000-0000-4000-8000-000000000002', 'johannesburg', 'The Living Room', 'Maboneng', 'nightlife', 'Rooftop lounge', 4.7, -26.2047, 28.0599, '12:00', '02:00', array['Rooftop', 'Cocktails'], true, 90, true),
  ('a1000000-0000-4000-8000-000000000003', 'johannesburg', 'Proud Mary', 'Rosebank', 'food', 'Modern dining', 4.7, -26.1456, 28.0425, '07:00', '23:00', array['Brunch', 'Cocktails'], true, 88, true),
  ('a1000000-0000-4000-8000-000000000004', 'johannesburg', 'Legends Barbershop', 'Braamfontein', 'prep', 'Barber', 4.6, -26.1923, 28.0308, '08:00', '19:00', array['Walk-ins', 'Fresh cuts'], false, 79, false),
  ('a1000000-0000-4000-8000-000000000005', 'johannesburg', 'Moya Yoga', 'Parkhurst', 'wellness', 'Yoga studio', 4.8, -26.1459, 28.0369, '06:00', '20:00', array['Yoga', 'Breathwork'], true, 86, false),
  ('a1000000-0000-4000-8000-000000000006', 'johannesburg', 'The Four Seasons', 'Westcliff', 'travel', 'Hotel', 4.8, -26.1685, 28.0353, null, null, array['City views', 'Dining'], true, 93, true),
  ('a1000000-0000-4000-8000-000000000007', 'johannesburg', 'McDonald''s', 'Braamfontein', 'always_open', '24-hour restaurant', 4.2, -26.1903, 28.0304, null, null, array['Food', 'Drive-through'], false, 60, false),
  ('a1000000-0000-4000-8000-000000000008', 'johannesburg', 'The Living Room', 'Maboneng', 'rooftops', 'Rooftop lounge', 4.7, -26.2047, 28.0599, '12:00', '02:00', array['Skyline', 'Cocktails'], true, 90, true),
  ('a1000000-0000-4000-8000-000000000009', 'johannesburg', 'Gold Reef City', 'Ormonde', 'casinos', 'Casino', 4.5, -26.2366, 28.0143, '09:00', '04:00', array['Gaming', 'Entertainment'], true, 85, false)
on conflict (id) do update set name = excluded.name, updated_at = now();

-- Relative schedules keep local development drops active and demonstrate clock-derived countdowns.
insert into public.flash_drops (id, city_id, source_name, source_verified, category, title, description, offer_text, address, latitude, longitude, starts_at, ends_at, created_at)
values
  ('b1000000-0000-4000-8000-000000000001', 'johannesburg', 'LIV Sandton', true, 'venue', 'FREE COCKTAIL', 'First 50 guests receive one complimentary cocktail.', 'COMPLIMENTARY COCKTAIL', '17 Sandton Drive, Sandton, 2196', -26.1076, 28.0567, now() - interval '10 minutes', now() + interval '18 minutes', now() - interval '5 minutes'),
  ('b1000000-0000-4000-8000-000000000002', 'johannesburg', 'The Den Randburg', true, 'venue', 'FREE ENTRY BEFORE 11PM', 'Skip the line and get in free before 11PM tonight.', 'FREE ENTRY', '30 Hill Street, Randburg, 2194', -26.0931, 27.9839, now() - interval '15 minutes', now() + interval '32 minutes', now() - interval '30 minutes'),
  ('b1000000-0000-4000-8000-000000000003', 'johannesburg', 'Rooftop Sundays', true, 'event', 'SURPRISE DJ SET', 'International DJ. Secret set. You have to be there.', 'LIVE DJ SET', '20 Anderson Street, Maboneng, 2094', -26.2035, 28.0605, now() - interval '5 minutes', now() + interval '67 minutes', now() - interval '15 minutes'),
  ('b1000000-0000-4000-8000-000000000004', 'johannesburg', 'Red Bull', true, 'brand', 'FREE SAMPLES', 'Energy up. Good times. Limited to first 100 people.', 'FREE SAMPLES', '1 Melrose Boulevard, Melrose, 2196', -26.1323, 28.0686, now() - interval '10 minutes', now() + interval '82 minutes', now() - interval '10 minutes')
on conflict (id) do update set starts_at = excluded.starts_at, ends_at = excluded.ends_at, created_at = excluded.created_at;

insert into public.prep_sessions (id, venue_name, starts_at, dress_code, ready_percent) values ('c1000000-0000-4000-8000-000000000001', 'AfroHaus Rooftop', now() + interval '3 hours', 'Smart casual', 84) on conflict (id) do update set starts_at = excluded.starts_at, ready_percent = excluded.ready_percent;
insert into public.prep_checklist_items (session_id, label, position) values ('c1000000-0000-4000-8000-000000000001', 'Ticket', 1), ('c1000000-0000-4000-8000-000000000001', 'Outfit', 2), ('c1000000-0000-4000-8000-000000000001', 'Phone charged', 3), ('c1000000-0000-4000-8000-000000000001', 'ID', 4) on conflict do nothing;

insert into public.smart_place_guides (id, city_id, title, context, mood, venue_count) values ('d1000000-0000-4000-8000-000000000001', 'johannesburg', 'Perfect First Date', 'Ideal before midnight', 'date_night', 10), ('d1000000-0000-4000-8000-000000000002', 'johannesburg', 'Hidden Rooftops', 'Open now', 'rooftops', 8) on conflict (id) do update set updated_at=now();
insert into public.smart_place_recommendations (guide_id, venue_id, reason, rank) values ('d1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000002', 'Romantic atmosphere · editor’s pick', 1), ('d1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000008', 'Amazing sunset views', 1) on conflict do nothing;
