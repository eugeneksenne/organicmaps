-- Contact cards are a first-class chat message kind.
-- ADD VALUE cannot be used in the same transaction as the new label on older Postgres;
-- it is committed as its own statement here and not referenced below.

alter type public.message_kind add value if not exists 'contact';
