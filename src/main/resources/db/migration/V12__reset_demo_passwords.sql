-- Reset demo staff passwords so Postman / frontend login works reliably.
-- Passwords (BCrypt strength 12, Spring-compatible $2a$):
--   invigilator@unza.zm  -> Invig@2026
--   invigilator2@unza.zm -> Invig2@2026
--   admin@unza.zm        -> Admin@2026
--   lecturer@unza.zm     -> Lect@2026

UPDATE public.staff
SET password_hash = '$2a$12$9XxZvT.Q24DDGq8pF4PEl.kIP3Njxt.OjKK01Up7Mhr4YL4vjZvaS'
WHERE email = 'invigilator@unza.zm';

UPDATE public.staff
SET password_hash = '$2a$12$TnlWyBK1MEh6hsbHypRPseEDTi/w49pCyp7n5GNnruiScx0G52HcW'
WHERE email = 'invigilator2@unza.zm';

UPDATE public.staff
SET password_hash = '$2a$12$mf2952B6Bv9F/lRUPR3A1.Q4Eee7FsrlrdwcX61xsfufOH8b44ug6'
WHERE email = 'admin@unza.zm';

UPDATE public.staff
SET password_hash = '$2a$12$YUSlbC6/wdIjSz6QwvtokeUb/u0h4pTxDHL.X1TY1aUQ3trqzJL8e'
WHERE email = 'lecturer@unza.zm';
