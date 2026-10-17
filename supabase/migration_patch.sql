-- ==============================================================================
-- SIAMD: Comprehensive & Verified Supabase Migration Patch
-- ==============================================================================
-- Run this in your Supabase Dashboard -> SQL Editor -> Run
-- ==============================================================================

-- 1. Ensure intern_groups table exists first
CREATE TABLE IF NOT EXISTS public.intern_groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    joining_date DATE NOT NULL,
    department TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.intern_groups ENABLE ROW LEVEL SECURITY;

-- 2. Ensure group_id and group_name columns exist on profiles
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'profiles' AND column_name = 'group_id') THEN
        ALTER TABLE public.profiles ADD COLUMN group_id TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'profiles' AND column_name = 'group_name') THEN
        ALTER TABLE public.profiles ADD COLUMN group_name TEXT;
    END IF;
END $$;

-- 3. Drop all policies first so column types can be altered without conflict
DROP POLICY IF EXISTS "Allow users to view own profile or admin view all" ON public.profiles;
DROP POLICY IF EXISTS "Allow authenticated read for groups" ON public.intern_groups;
DROP POLICY IF EXISTS "Allow admin manage groups" ON public.intern_groups;
DROP POLICY IF EXISTS "Allow authenticated read for holidays" ON public.holiday_calendar;
DROP POLICY IF EXISTS "Intern view own balance, Admin view all" ON public.leave_balances;
DROP POLICY IF EXISTS "Intern view own leave applications" ON public.leave_applications;
DROP POLICY IF EXISTS "Intern insert own leave application" ON public.leave_applications;
DROP POLICY IF EXISTS "Admin update leave status" ON public.leave_applications;

DROP POLICY IF EXISTS "Allow read groups" ON public.intern_groups;
DROP POLICY IF EXISTS "Allow manage groups" ON public.intern_groups;
DROP POLICY IF EXISTS "Allow read holidays" ON public.holiday_calendar;
DROP POLICY IF EXISTS "Allow read profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow insert profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow update profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow delete profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow read attendance" ON public.attendance_logs;
DROP POLICY IF EXISTS "Allow insert attendance" ON public.attendance_logs;
DROP POLICY IF EXISTS "Allow update attendance" ON public.attendance_logs;
DROP POLICY IF EXISTS "Allow read leave balances" ON public.leave_balances;
DROP POLICY IF EXISTS "Allow manage leave balances" ON public.leave_balances;
DROP POLICY IF EXISTS "Allow read leave applications" ON public.leave_applications;
DROP POLICY IF EXISTS "Allow insert leave applications" ON public.leave_applications;
DROP POLICY IF EXISTS "Allow update leave applications" ON public.leave_applications;

-- 4. Drop ALL foreign keys referencing profiles(id)
ALTER TABLE public.attendance_logs DROP CONSTRAINT IF EXISTS attendance_logs_intern_id_fkey;
ALTER TABLE public.leave_balances DROP CONSTRAINT IF EXISTS leave_balances_intern_id_fkey;
ALTER TABLE public.leave_applications DROP CONSTRAINT IF EXISTS leave_applications_intern_id_fkey;
ALTER TABLE public.leave_applications DROP CONSTRAINT IF EXISTS leave_applications_reviewed_by_fkey;
ALTER TABLE public.stipend_computations DROP CONSTRAINT IF EXISTS stipend_computations_intern_id_fkey;
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;

-- 5. Alter all ID & reference columns to TEXT (avoids UUID mismatch)
ALTER TABLE public.profiles ALTER COLUMN id TYPE TEXT USING id::text;
ALTER TABLE public.attendance_logs ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;
ALTER TABLE public.leave_balances ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;
ALTER TABLE public.leave_applications ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;
ALTER TABLE public.leave_applications ALTER COLUMN reviewed_by TYPE TEXT USING reviewed_by::text;
ALTER TABLE public.stipend_computations ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;

-- 6. Re-add foreign key constraints to public.profiles(id)
ALTER TABLE public.attendance_logs ADD CONSTRAINT attendance_logs_intern_id_fkey FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE public.leave_balances ADD CONSTRAINT leave_balances_intern_id_fkey FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE public.leave_applications ADD CONSTRAINT leave_applications_intern_id_fkey FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;
ALTER TABLE public.leave_applications ADD CONSTRAINT leave_applications_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.profiles(id) ON DELETE SET NULL;
ALTER TABLE public.stipend_computations ADD CONSTRAINT stipend_computations_intern_id_fkey FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

-- 7. Grant permissions to anon and authenticated roles
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- 8. Add live policies for mobile app client (using Anon Key)
CREATE POLICY "Allow read groups" ON public.intern_groups FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Allow manage groups" ON public.intern_groups FOR ALL TO anon, authenticated USING (true);

CREATE POLICY "Allow read holidays" ON public.holiday_calendar FOR SELECT TO anon, authenticated USING (true);

CREATE POLICY "Allow read profiles" ON public.profiles FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Allow insert profiles" ON public.profiles FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Allow update profiles" ON public.profiles FOR UPDATE TO anon, authenticated USING (true);
CREATE POLICY "Allow delete profiles" ON public.profiles FOR DELETE TO anon, authenticated USING (true);

CREATE POLICY "Allow read attendance" ON public.attendance_logs FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Allow insert attendance" ON public.attendance_logs FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Allow update attendance" ON public.attendance_logs FOR UPDATE TO anon, authenticated USING (true);

CREATE POLICY "Allow read leave balances" ON public.leave_balances FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Allow manage leave balances" ON public.leave_balances FOR ALL TO anon, authenticated USING (true);

CREATE POLICY "Allow read leave applications" ON public.leave_applications FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Allow insert leave applications" ON public.leave_applications FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Allow update leave applications" ON public.leave_applications FOR UPDATE TO anon, authenticated USING (true);
