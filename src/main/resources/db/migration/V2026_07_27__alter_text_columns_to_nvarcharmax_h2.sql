-- ============================================================================
-- Migration: Fix NCLOB to NVARCHAR(MAX) for H2 database
-- H2 does not have NCLOB issues but we need to ensure consistency.
-- NVARCHAR is mapped to VARCHAR in H2 which works fine.
-- ============================================================================

-- H2 uses VARCHAR/CLOB for text columns. The application code changes
-- use columnDefinition = "nvarchar(max)" which H2 maps to VARCHAR(4000).
-- This migration is mostly a no-op for H2 but ensures compatibility.
