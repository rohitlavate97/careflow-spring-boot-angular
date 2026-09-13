package com.careflow.clinical.domain;

/**
 * Clinical note category following medical documentation standards (§23).
 */
public enum NoteType {
    /**
     * Standard clinical summary or general evaluation entry.
     */
    GENERAL,

    /**
     * SOAP framework: Subjective findings (patient statements, symptoms).
     */
    SOAP_SUBJECTIVE,

    /**
     * SOAP framework: Objective findings (physical exam, lab results, measurements).
     */
    SOAP_OBJECTIVE,

    /**
     * SOAP framework: Assessment (medical analysis, diagnostic formulation).
     */
    SOAP_ASSESSMENT,

    /**
     * SOAP framework: Plan (therapeutic plan, orders, referrals).
     */
    SOAP_PLAN,

    /**
     * Clinical progress observation over ongoing treatment course.
     */
    PROGRESS_NOTE
}
