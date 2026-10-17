-- =====================================================================
-- SAIL INTERN ATTENDANCE MANAGEMENT APPLICATION (SIAMD)
-- AUTOMATED ABSENTEEISM SCHEDULER (Supabase pg_cron / Edge Function)
-- =====================================================================

-- 1. Create the Stored Procedure to Auto-Mark Absenteeism
-- This function runs daily at the specified cutoff time (e.g., 18:00 IST / Midnight).
-- Any intern who has not scanned today's Secret Attendance QR code is marked ABSENT.
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
    -- Check if target_date is Sunday (EXTRACT(DOW FROM ...) returns 0 for Sunday)
    is_sunday := (EXTRACT(DOW FROM target_date) = 0);
    
    -- Check if target_date is an official SAIL holiday
    SELECT EXISTS (
        SELECT 1 FROM public.holiday_calendar WHERE holiday_date = target_date
    ) INTO is_sail_holiday;

    -- If it's Sunday or an official Holiday, interns are not marked absent
    IF is_sunday OR is_sail_holiday THEN
        RETURN QUERY SELECT 0, target_date;
        RETURN;
    END IF;

    -- Insert ABSENT records for all active interns who have not scanned in for today
    WITH unmarked_interns AS (
        SELECT p.id AS intern_id
        FROM public.profiles p
        WHERE p.role = 'intern'
          AND p.joining_date <= target_date
          AND NOT EXISTS (
              SELECT 1 
              FROM public.attendance_logs a 
              WHERE a.intern_id = p.id 
                AND a.record_date = target_date
          )
    )
    INSERT INTO public.attendance_logs (
        id,
        intern_id,
        record_date,
        status,
        check_in_time,
        marked_by,
        remarks,
        created_at
    )
    SELECT
        intern_id || '-' || target_date::TEXT,
        intern_id,
        target_date,
        'ABSENT',
        NULL,
        'Automated Absenteeism Scheduler (pg_cron)',
        'Auto-marked absent: Daily QR attendance was not scanned before cutoff time.',
        NOW()
    FROM unmarked_interns
    ON CONFLICT (id) DO NOTHING;

    GET DIAGNOSTICS inserted_rows = ROW_COUNT;

    RETURN QUERY SELECT inserted_rows, target_date;
END;
$$;

-- Grant execution permissions
GRANT EXECUTE ON FUNCTION public.auto_mark_daily_absenteeism(DATE) TO postgres, anon, authenticated, service_role;

-- =====================================================================
-- 2. Schedule with pg_cron (Daily Cutoff at 18:30 IST / 13:00 UTC)
-- Note: Enable the pg_cron extension from your Supabase Dashboard:
-- Database -> Extensions -> search for "pg_cron" -> Enable.
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Remove any old scheduled job if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'sail_daily_absenteeism_cutoff') THEN
        PERFORM cron.unschedule('sail_daily_absenteeism_cutoff');
    END IF;
END $$;

-- Schedule job to run daily at 18:00 IST (12:30 UTC):
-- '30 12 * * 1-6' runs Monday through Saturday at 12:30 UTC (18:00 IST)
SELECT cron.schedule(
    'sail_daily_absenteeism_cutoff',
    '30 12 * * 1-6',
    'SELECT public.auto_mark_daily_absenteeism(CURRENT_DATE);'
);

-- =====================================================================
-- 3. Verification Query:
-- Run this query manually in Supabase SQL Editor to test absenteeism for today:
-- SELECT * FROM public.auto_mark_daily_absenteeism(CURRENT_DATE);
-- =====================================================================
