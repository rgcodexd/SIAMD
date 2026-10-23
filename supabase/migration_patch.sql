-- ==============================================================================
-- SAIL INTERN ATTENDANCE MANAGEMENT APPLICATION (SIAMD)
-- COMPREHENSIVE ALL-IN-ONE SUPABASE MIGRATION & SCHEDULER PATCH
-- ==============================================================================
-- INSTRUCTIONS:
-- 1. Open your Supabase Dashboard: https://supabase.com/dashboard/project/ftnueczhuwoxlmllvkgt
-- 2. Go to "SQL Editor" in the left sidebar.
-- 3. Click "New query", paste this entire script, and click "Run".
-- ==============================================================================

-- 1. Ensure extensions exist
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_cron";

-- 2. Ensure intern_groups table exists
CREATE TABLE IF NOT EXISTS public.intern_groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    joining_date DATE NOT NULL,
    department TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.intern_groups ENABLE ROW LEVEL SECURITY;

-- 2b. Ensure departments table exists (Admin-managed departments)
CREATE TABLE IF NOT EXISTS public.departments (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    code TEXT,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.departments ENABLE ROW LEVEL SECURITY;

-- 3. Ensure profiles table has required columns
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'group_id') THEN
        ALTER TABLE public.profiles ADD COLUMN group_id TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'group_name') THEN
        ALTER TABLE public.profiles ADD COLUMN group_name TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'auth_provider') THEN
        ALTER TABLE public.profiles ADD COLUMN auth_provider TEXT DEFAULT 'EMAIL';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'college') THEN
        ALTER TABLE public.profiles ADD COLUMN college TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'phone') THEN
        ALTER TABLE public.profiles ADD COLUMN phone TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'emergency_contact') THEN
        ALTER TABLE public.profiles ADD COLUMN emergency_contact TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'address') THEN
        ALTER TABLE public.profiles ADD COLUMN address TEXT;
    END IF;
END $$;

-- 4. Drop all existing RLS policies on tables to prevent dependency locks during type conversion
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
DROP POLICY IF EXISTS "Allow read departments" ON public.departments;
DROP POLICY IF EXISTS "Allow insert departments" ON public.departments;
DROP POLICY IF EXISTS "Allow update departments" ON public.departments;
DROP POLICY IF EXISTS "Allow delete departments" ON public.departments;
DROP POLICY IF EXISTS "Allow manage departments" ON public.departments;
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

-- 5. Drop ALL foreign key constraints that reference public.profiles(id)
-- This avoids ERROR 42804 (incompatible types uuid and text) when altering column types
ALTER TABLE public.attendance_logs DROP CONSTRAINT IF EXISTS attendance_logs_intern_id_fkey;
ALTER TABLE public.leave_balances DROP CONSTRAINT IF EXISTS leave_balances_intern_id_fkey;
ALTER TABLE public.leave_applications DROP CONSTRAINT IF EXISTS leave_applications_intern_id_fkey;
ALTER TABLE public.leave_applications DROP CONSTRAINT IF EXISTS leave_applications_reviewed_by_fkey;
ALTER TABLE public.stipend_computations DROP CONSTRAINT IF EXISTS stipend_computations_intern_id_fkey;
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;

-- 6. Harmonize all ID and reference columns to TEXT type
ALTER TABLE public.profiles ALTER COLUMN id TYPE TEXT USING id::text;
ALTER TABLE public.attendance_logs ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;
ALTER TABLE public.leave_balances ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;
ALTER TABLE public.leave_applications ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;
ALTER TABLE public.leave_applications ALTER COLUMN reviewed_by TYPE TEXT USING reviewed_by::text;
ALTER TABLE public.stipend_computations ALTER COLUMN intern_id TYPE TEXT USING intern_id::text;

-- 7. Re-add consistent foreign key constraints with ON DELETE rules
ALTER TABLE public.attendance_logs 
    ADD CONSTRAINT attendance_logs_intern_id_fkey 
    FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

ALTER TABLE public.leave_balances 
    ADD CONSTRAINT leave_balances_intern_id_fkey 
    FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

ALTER TABLE public.leave_applications 
    ADD CONSTRAINT leave_applications_intern_id_fkey 
    FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

ALTER TABLE public.leave_applications 
    ADD CONSTRAINT leave_applications_reviewed_by_fkey 
    FOREIGN KEY (reviewed_by) REFERENCES public.profiles(id) ON DELETE SET NULL;

ALTER TABLE public.stipend_computations 
    ADD CONSTRAINT stipend_computations_intern_id_fkey 
    FOREIGN KEY (intern_id) REFERENCES public.profiles(id) ON DELETE CASCADE;

-- 8. Grant permissions to anon and authenticated roles for mobile client access
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO anon, authenticated;

-- 9. Add permissive Row Level Security (RLS) policies for mobile client
CREATE POLICY "Allow read groups" ON public.intern_groups FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Allow manage groups" ON public.intern_groups FOR ALL TO anon, authenticated USING (true);

CREATE POLICY "Allow read departments" ON public.departments FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Allow insert departments" ON public.departments FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Allow update departments" ON public.departments FOR UPDATE TO anon, authenticated USING (true);
CREATE POLICY "Allow delete departments" ON public.departments FOR DELETE TO anon, authenticated USING (true);

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

-- 10. Default Admin Record (admin@sail.in / admin123)
INSERT INTO public.profiles (
    id, email, full_name, role, intern_id_number, department, internship_start_date, internship_end_date
) VALUES (
    'SAIL-ADM-001', 'admin@sail.in', 'SAIL Plant Administrator', 'ADMIN', 'ADM-001', 'HRD Department', '2025-01-01', '2030-12-31'
) ON CONFLICT (id) DO NOTHING;

-- 11. Stored Procedure for Automated Daily Absenteeism
-- Automatically marks active interns as ABSENT if they did not scan today's QR code before cutoff
CREATE OR REPLACE FUNCTION public.auto_mark_daily_absenteeism(target_date DATE DEFAULT CURRENT_DATE)
RETURNS TABLE(marked_count INT, target DATE) 
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    is_sunday BOOLEAN;
    is_sail_holiday BOOLEAN;
    inserted_rows INT := 0;
BEGIN
    is_sunday := (EXTRACT(DOW FROM target_date) = 0);
    
    SELECT EXISTS (
        SELECT 1 FROM public.holiday_calendar WHERE holiday_date = target_date
    ) INTO is_sail_holiday;

    IF is_sunday OR is_sail_holiday THEN
        RETURN QUERY SELECT 0, target_date;
        RETURN;
    END IF;

    WITH unmarked_interns AS (
        SELECT p.id AS intern_id
        FROM public.profiles p
        WHERE p.role = 'INTERN'
          AND p.internship_start_date <= target_date
          AND NOT EXISTS (
              SELECT 1 
              FROM public.attendance_logs a 
              WHERE a.intern_id = p.id 
                AND a.attendance_date = target_date
          )
    )
    INSERT INTO public.attendance_logs (
        id,
        intern_id,
        attendance_date,
        status,
        scan_time,
        location_verified,
        created_at
    )
    SELECT
        uuid_generate_v4(),
        intern_id,
        target_date,
        'ABSENT',
        NULL,
        FALSE,
        NOW()
    FROM unmarked_interns
    ON CONFLICT (intern_id, attendance_date) DO NOTHING;

    GET DIAGNOSTICS inserted_rows = ROW_COUNT;

    RETURN QUERY SELECT inserted_rows, target_date;
END;
$$;

GRANT EXECUTE ON FUNCTION public.auto_mark_daily_absenteeism(DATE) TO postgres, anon, authenticated, service_role;

-- 12. Automated Scheduler Job (Runs Monday to Saturday at 18:00 IST / 12:30 UTC)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'sail_daily_absenteeism_cutoff') THEN
        PERFORM cron.unschedule('sail_daily_absenteeism_cutoff');
    END IF;
EXCEPTION
    WHEN undefined_table THEN
        -- pg_cron not active yet; user can enable in Supabase Database Extensions
        NULL;
END $$;

DO $$
BEGIN
    PERFORM cron.schedule(
        'sail_daily_absenteeism_cutoff',
        '30 12 * * 1-6',
        'SELECT public.auto_mark_daily_absenteeism(CURRENT_DATE);'
    );
EXCEPTION
    WHEN OTHERS THEN
        NULL;
END $$;
