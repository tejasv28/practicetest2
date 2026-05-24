package com.bfhl.solution.service;

import org.springframework.stereotype.Service;

/**
 * Resolves which SQL question to answer and returns the corresponding query.
 *
 * Question assignment rule (per problem statement):
 *   last two digits of regNo are ODD  → Question 1
 *   last two digits of regNo are EVEN → Question 2
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * QUESTION 1
 * Find the 2nd highest salary from the EMPLOYEES table without using
 * LIMIT/TOP or any ranking window function directly on the final result.
 *
 * QUESTION 2
 * For each department, find employees whose salary is greater than
 * the average salary of that department. Return employee name, salary,
 * and department name ordered by department name then salary descending.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
public class SqlResolverService {

    // ── SQL for Question 1 ────────────────────────────────────────────────────
    // Approach: use a correlated sub-query that counts how many distinct
    // salaries are greater than the current row's salary.  When that count
    // equals exactly 1, the current row holds the 2nd-highest salary.
    private static final String QUESTION_1_QUERY =
            "SELECT e.AMOUNT AS SALARY " +
            "FROM PAYMENTS e " +
            "WHERE 2 = (" +
            "    SELECT COUNT(DISTINCT e2.AMOUNT) " +
            "    FROM PAYMENTS e2 " +
            "    WHERE e2.AMOUNT >= e.AMOUNT" +
            ");";

    // ── SQL for Question 2 ────────────────────────────────────────────────────
    // Approach: join EMPLOYEE_DETAILS with a derived table that computes the
    // per-department average, then keep only rows whose salary beats it.
    private static final String QUESTION_2_QUERY =
            "SELECT e.NAME, e.AMOUNT, e.DEPARTMENT " +
            "FROM EMPLOYEE_DETAILS e " +
            "JOIN (" +
            "    SELECT DEPARTMENT, AVG(AMOUNT) AS AVG_AMOUNT " +
            "    FROM EMPLOYEE_DETAILS " +
            "    GROUP BY DEPARTMENT" +
            ") dept_avg ON e.DEPARTMENT = dept_avg.DEPARTMENT " +
            "WHERE e.AMOUNT > dept_avg.AVG_AMOUNT " +
            "ORDER BY e.DEPARTMENT ASC, e.AMOUNT DESC;";

    /**
     * Returns the SQL query string that matches the registration number.
     *
     * @param regNo  the candidate's registration number (e.g. "REG12347")
     * @return       the SQL query to submit
     */
    public String resolveQuery(String regNo) {
        int lastTwoDigits = extractLastTwoDigits(regNo);
        boolean isOdd     = (lastTwoDigits % 2 != 0);

        if (isOdd) {
            System.out.println("[SqlResolverService] regNo last two digits = "
                    + lastTwoDigits + " (ODD)  → selecting Question 1");
            return QUESTION_1_QUERY;
        } else {
            System.out.println("[SqlResolverService] regNo last two digits = "
                    + lastTwoDigits + " (EVEN) → selecting Question 2");
            return QUESTION_2_QUERY;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the numeric suffix from a registration number and returns the
     * last two digits.  E.g. "REG12347" → 47, "REG001" → 1, "12" → 12.
     */
    private int extractLastTwoDigits(String regNo) {
        // Strip any non-digit leading characters (e.g. "REG")
        String digitsOnly = regNo.replaceAll("[^0-9]", "");

        if (digitsOnly.isEmpty()) {
            throw new IllegalArgumentException(
                    "Registration number contains no digits: " + regNo);
        }

        // Take the last two characters and parse them as an integer
        String lastTwo = digitsOnly.length() >= 2
                ? digitsOnly.substring(digitsOnly.length() - 2)
                : digitsOnly;

        return Integer.parseInt(lastTwo);
    }
}
