-- ==============================================================================
-- SAIL Intern Attendance Management Application (SIAMD)
-- Supabase / PostgreSQL Database Schema for Leaves, Holidays, and Stipend
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. ENUMS
CREATE TYPE user_role AS ENUM ('INTERN', 'ADMIN', 'SUPERVISOR');
CREATE TYPE leave_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');
CREATE TYPE holiday_category AS ENUM (
    'NATIONAL_HOLIDAY',            -- 03 Days (Republic Day, Independence Day, Gandhi Jayanti)
    'SAIL_FOUNDATION_DAY',         -- 01 Day (24th January Closed Holiday)
    'FESTIVAL_HOLIDAY',            -- 06 Days (Holi, Eid-ul-Fitr, Dussehra, Diwali, Guru Nanak Jayanti, etc.)
    'AMBEDKAR_JAYANTI'             -- 01 Day (14th April - Govt declared)
);
CREATE TYPE attendance_status AS ENUM ('PRESENT', 'ABSENT', 'ON_LEAVE', 'HOLIDAY', 'HALF_DAY');

-- 3. PROFILES / USERS
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    role user_role NOT NULL DEFAULT 'INTERN',
    intern_id_number TEXT UNIQUE,           -- e.g., SAIL-INT-2025-001
    department TEXT NOT NULL DEFAULT 'Plant Engineering',
    mentor_name TEXT,
    phone TEXT,
    college TEXT,
    emergency_contact TEXT,
    address TEXT,
    group_id TEXT,
    group_name TEXT,
    stipend_monthly NUMERIC(10, 2) NOT NULL DEFAULT 15000.00,
    internship_start_date DATE NOT NULL,
    internship_end_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3b. INTERN GROUPS / BOOKS (Categorized by Joining Date)
CREATE TABLE IF NOT EXISTS public.intern_groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    joining_date DATE NOT NULL,
    department TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3c. DEPARTMENTS (Created dynamically by Admin)
CREATE TABLE IF NOT EXISTS public.departments (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    code TEXT,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. OFFICIAL SAIL HOLIDAY CALENDAR
-- Enforces policy: 3 National + 1 SAIL Day + 6 Festival + 1 Ambedkar Jayanti = 11 holidays
CREATE TABLE IF NOT EXISTS public.holiday_calendar (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    calendar_year INT NOT NULL,
    title TEXT NOT NULL,
    holiday_date DATE NOT NULL,
    category holiday_category NOT NULL,
    is_closed_holiday BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_holiday_per_date UNIQUE(calendar_year, holiday_date, title)
);

-- 5. INTERN LEAVE BALANCES
-- Enforces SAIL Entitlement: 12 Sanctioned Leaves admissible during internship period
CREATE TABLE IF NOT EXISTS public.leave_balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intern_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    total_sanctioned_quota NUMERIC(4, 1) NOT NULL DEFAULT 12.0, -- SAIL standard: 12 (Twelve) days
    used_leaves NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    pending_leaves NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    academic_year INT NOT NULL DEFAULT EXTRACT(YEAR FROM CURRENT_DATE),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_intern_leave_year UNIQUE(intern_id, academic_year),
    CONSTRAINT check_leaves_positive CHECK (used_leaves >= 0 AND total_sanctioned_quota >= 0)
);

-- 6. LEAVE APPLICATIONS
CREATE TABLE IF NOT EXISTS public.leave_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intern_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days NUMERIC(4, 1) NOT NULL,
    reason TEXT NOT NULL,
    status leave_status NOT NULL DEFAULT 'PENDING',
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    reviewed_by TEXT REFERENCES public.profiles(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    admin_remarks TEXT,
    CONSTRAINT check_date_range CHECK (end_date >= start_date),
    CONSTRAINT check_positive_days CHECK (total_days > 0)
);

-- 7. ATTENDANCE & QR SCANNING LOGS
CREATE TABLE IF NOT EXISTS public.attendance_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intern_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    status attendance_status NOT NULL DEFAULT 'PRESENT',
    scan_time TIMESTAMP WITH TIME ZONE,
    qr_session_code TEXT,
    location_verified BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_intern_daily_attendance UNIQUE(intern_id, attendance_date)
);

-- 8. MONTHLY STIPEND COMPUTATION RECORD
CREATE TABLE IF NOT EXISTS public.stipend_computations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intern_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    month_year DATE NOT NULL, -- First day of calculation month (e.g., '2025-05-01')
    working_days INT NOT NULL DEFAULT 22,
    days_present INT NOT NULL DEFAULT 0,
    admissible_leaves_used NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    holidays_count INT NOT NULL DEFAULT 0,
    loss_of_pay_days NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    base_stipend NUMERIC(10, 2) NOT NULL,
    deduction_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    net_payable_stipend NUMERIC(10, 2) NOT NULL,
    status TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT, APPROVED, DISBURSED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_monthly_stipend UNIQUE(intern_id, month_year)
);

-- ==============================================================================
-- DEFAULT SEED DATA: SAIL 2025 OFFICIAL HOLIDAYS (11 Entitled Days)
-- ==============================================================================
INSERT INTO public.holiday_calendar (calendar_year, title, holiday_date, category, description)
VALUES
    -- b. National Holiday Leaves (3 days)
    (2025, 'Republic Day', '2025-01-26', 'NATIONAL_HOLIDAY', 'National Holiday - Republic Day celebration'),
    (2025, 'Independence Day', '2025-08-15', 'NATIONAL_HOLIDAY', 'National Holiday - 78th Independence Day'),
    (2025, 'Mahatma Gandhi Jayanti', '2025-10-02', 'NATIONAL_HOLIDAY', 'National Holiday - Gandhi Jayanti'),

    -- c. SAIL Foundation Day Closed Holiday (1 day)
    (2025, 'SAIL Foundation Day', '2025-01-24', 'SAIL_FOUNDATION_DAY', 'Closed Holiday on account of SAIL Foundation Day'),

    -- d. Festival Holidays (FHs) (6 days specified for 2025)
    (2025, 'Holi', '2025-03-14', 'FESTIVAL_HOLIDAY', 'Festival of Colours'),
    (2025, 'Eid-ul-Fitr', '2025-03-31', 'FESTIVAL_HOLIDAY', 'Festival Holiday'),
    (2025, 'Dussehra (Vijaya Dashami)', '2025-10-02', 'FESTIVAL_HOLIDAY', 'Festival Holiday - Dussehra celebration'),
    (2025, 'Diwali (Deepawali)', '2025-10-20', 'FESTIVAL_HOLIDAY', 'Festival of Lights'),
    (2025, 'Guru Nanak Jayanti', '2025-11-05', 'FESTIVAL_HOLIDAY', 'Prakash Utsav'),
    (2025, 'Christmas Day', '2025-12-25', 'FESTIVAL_HOLIDAY', 'Festival Holiday (Winter FH)'),

    -- e. Ambedkar Jayanti Holiday (1 day)
    (2025, 'Dr. B.R. Ambedkar Jayanti', '2025-04-14', 'AMBEDKAR_JAYANTI', 'Govt declared closed holiday honoring Dr. B.R. Ambedkar')
ON CONFLICT (calendar_year, holiday_date, title) DO NOTHING;

-- ==============================================================================
-- DATABASE TRIGGERS & BUSINESS LOGIC
-- ==============================================================================

-- Trigger: Automatically initialize 12 sanctioned leaves for newly enrolled interns
CREATE OR REPLACE FUNCTION public.handle_new_intern_profile()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.role = 'INTERN' THEN
        INSERT INTO public.leave_balances (intern_id, total_sanctioned_quota, used_leaves, pending_leaves)
        VALUES (NEW.id, 12.0, 0.0, 0.0)
        ON CONFLICT (intern_id, academic_year) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_intern_profile_created ON public.profiles;
CREATE TRIGGER on_intern_profile_created
    AFTER INSERT ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_intern_profile();

-- Function: Manage leave balance deductions upon application status updates
CREATE OR REPLACE FUNCTION public.handle_leave_status_change()
RETURNS TRIGGER AS $$
BEGIN
    -- If newly submitted as PENDING, increment pending leaves
    IF (TG_OP = 'INSERT' AND NEW.status = 'PENDING') THEN
        UPDATE public.leave_balances
        SET pending_leaves = pending_leaves + NEW.total_days,
            updated_at = NOW()
        WHERE intern_id = NEW.intern_id;

    -- If approved from pending: move from pending to used
    ELSIF (TG_OP = 'UPDATE' AND OLD.status = 'PENDING' AND NEW.status = 'APPROVED') THEN
        UPDATE public.leave_balances
        SET pending_leaves = GREATEST(0, pending_leaves - OLD.total_days),
            used_leaves = used_leaves + NEW.total_days,
            updated_at = NOW()
        WHERE intern_id = NEW.intern_id;

        -- Record attendance status as ON_LEAVE for these dates
        INSERT INTO public.attendance_logs (intern_id, attendance_date, status)
        SELECT 
            NEW.intern_id, 
            d::DATE, 
            'ON_LEAVE'::attendance_status
        FROM generate_series(NEW.start_date, NEW.end_date, '1 day'::interval) d
        ON CONFLICT (intern_id, attendance_date) 
        DO UPDATE SET status = 'ON_LEAVE';

    -- If rejected from pending: remove from pending
    ELSIF (TG_OP = 'UPDATE' AND OLD.status = 'PENDING' AND NEW.status = 'REJECTED') THEN
        UPDATE public.leave_balances
        SET pending_leaves = GREATEST(0, pending_leaves - OLD.total_days),
            updated_at = NOW()
        WHERE intern_id = NEW.intern_id;

    -- If cancelled from approved: deduct from used
    ELSIF (TG_OP = 'UPDATE' AND OLD.status = 'APPROVED' AND NEW.status = 'CANCELLED') THEN
        UPDATE public.leave_balances
        SET used_leaves = GREATEST(0, used_leaves - OLD.total_days),
            updated_at = NOW()
        WHERE intern_id = NEW.intern_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_leave_status_change ON public.leave_applications;
CREATE TRIGGER on_leave_status_change
    AFTER INSERT OR UPDATE ON public.leave_applications
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_leave_status_change();

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.holiday_calendar ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leave_balances ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leave_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stipend_computations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.intern_groups ENABLE ROW LEVEL SECURITY;

-- Groups: Everyone can read and manage
CREATE POLICY "Allow read groups"
    ON public.intern_groups FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow manage groups"
    ON public.intern_groups FOR ALL
    TO anon, authenticated
    USING (true);

-- Everyone authenticated can view holidays
CREATE POLICY "Allow read holidays"
    ON public.holiday_calendar FOR SELECT
    TO anon, authenticated
    USING (true);

-- Grant Permissions
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- Profiles: Allow read, insert, update, delete
CREATE POLICY "Allow read profiles"
    ON public.profiles FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert profiles"
    ON public.profiles FOR INSERT
    TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update profiles"
    ON public.profiles FOR UPDATE
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow delete profiles"
    ON public.profiles FOR DELETE
    TO anon, authenticated
    USING (true);

-- Attendance Logs: Allow read, insert, update
CREATE POLICY "Allow read attendance"
    ON public.attendance_logs FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert attendance"
    ON public.attendance_logs FOR INSERT
    TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update attendance"
    ON public.attendance_logs FOR UPDATE
    TO anon, authenticated
    USING (true);

-- Leave Balances: Allow read and manage
CREATE POLICY "Allow read leave balances"
    ON public.leave_balances FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow manage leave balances"
    ON public.leave_balances FOR ALL
    TO anon, authenticated
    USING (true);

-- Leave Applications: Allow read, insert, update
CREATE POLICY "Allow read leave applications"
    ON public.leave_applications FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert leave applications"
    ON public.leave_applications FOR INSERT
    TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update leave applications"
    ON public.leave_applications FOR UPDATE
    TO anon, authenticated
    USING (true);
