# Tasks: [Change Title]

> **Change ID:** `_example`
> **Linked Proposal:** [proposal.md](./proposal.md)
> **Status:** in-progress | complete
>
> All tasks must be `[x]` before this change can be archived via `sdd-spec-archive`.

---

## Implementation Tasks

- [ ] <!-- Task 1: short imperative description, e.g. "Create UserAuthService class" -->
- [ ] <!-- Task 2 -->
- [ ] <!-- Task 3 -->

## Security Validation Tasks

- [ ] <!-- Verify input validation is implemented as described in proposal's Security section -->
- [ ] <!-- Confirm no secrets are hardcoded (run: grep -r "password\|secret\|token" src/ --include="*.ts") -->
- [ ] <!-- Verify authorization checks are in place for all new endpoints -->

## Shift-Left Test Suite Tasks

### Happy Path
- [ ] <!-- e.g. "Write unit test TP-01: UserAuthService.login() with valid credentials" -->
- [ ] <!-- e.g. "Write integration test TP-02: POST /auth/login returns 200 with valid JWT" -->

### Edge Cases & Failure Modes
- [ ] <!-- e.g. "Write unit test TE-01: UserAuthService.login() with null password throws ValidationError" -->
- [ ] <!-- e.g. "Write test TE-02: POST /auth/login with expired token returns 401" -->

## Documentation Tasks

- [ ] <!-- e.g. "Update openapi.yaml with /auth/login schema" -->
- [ ] <!-- e.g. "Update README with auth setup instructions" -->

## Review Gate

- [ ] All implementation tasks complete
- [ ] All security validation tasks complete
- [ ] All tests passing (`<test command>`)
- [ ] Proposal acceptance criteria verified (run sdd-spec-validate)
- [ ] Ready for `sdd-spec-archive`
