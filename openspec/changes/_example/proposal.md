# Proposal: [Change Title]

> **Change ID:** `_example`
> **Status:** draft | under-review | approved | rejected
> **Author:** <!-- your name or @handle -->
> **Created:** <!-- YYYY-MM-DD -->
> **Last Updated:** <!-- YYYY-MM-DD -->

---

## Summary

<!-- One paragraph describing what this change does and why it is needed. -->

## Motivation

<!-- What problem does this solve? What user need or business requirement drives it?
     Include links to issues, tickets, or discussions if applicable. -->

## Scope

### In Scope
- <!-- Bullet list of what IS included in this change -->

### Out of Scope
- <!-- Bullet list of what is explicitly NOT included -->

## Proposed Solution

<!-- Describe the approach at a high level. Reference impacted files if known.
     The sdd-code-explorer subagent will populate this section automatically when
     sdd-spec-create is used. -->

### Impacted Files
| File | Change Type | Notes |
|------|-------------|-------|
| `path/to/file` | Modify | <!-- brief note --> |

## Acceptance Criteria

<!-- Written as Given-When-Then verifiable checkboxes: -->
- [ ] **Given** <!-- context --> **When** <!-- action --> **Then** <!-- expected result -->
- [ ] <!-- Criterion 2 -->

## Defensive Security & Validation Considerations

### Input Validation
- <!-- How incoming data is validated -->

### Secret Isolation
- <!-- Confirm no hardcoded secrets; env variable strategy -->

### Authorization & Authentication Impact
- <!-- Auth impact or "N/A" -->

## Testing Strategy & Shift-Left Plan

### Happy Path Tests
| Test ID | Scenario | Test Type | Expected Result |
|---------|----------|-----------|-----------------|
| TP-01 | <!-- Normal usage --> | Unit / Integration / E2E | <!-- Outcome --> |

### Edge Case & Failure Mode Tests
| Test ID | Scenario | Test Type | Expected Result |
|---------|----------|-----------|-----------------|
| TE-01 | <!-- Boundary or failure scenario --> | Unit / Integration | <!-- Outcome --> |

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| <!-- risk --> | Low/Med/High | Low/Med/High | <!-- mitigation --> |

## References

- <!-- Links to related specs, ADRs, or documentation -->
