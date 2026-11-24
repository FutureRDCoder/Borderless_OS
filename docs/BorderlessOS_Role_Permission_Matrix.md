# BorderlessOS --- Role-Permission Matrix

**Project:** Cross-Border Remittance & Compliance Operations Platform\
**Version:** 1.1 --- Approved implementation baseline\
**Status:** Approved; conditional permissions are limited to the
implemented workflows described below\
**Scope:** Five-day educational prototype

## 1. Purpose

This document defines the initial permissions and user journeys for four
human roles: `CUSTOMER`, `OPERATIONS`, `COMPLIANCE`, and `ADMIN`. It is
an implementation baseline for backend authorisation, API access,
workflow validation, and security tests. It is not a production
remittance control framework or certified KYC/AML system.

## 2. Permission legend

-   **Allowed** --- the role may perform the action within the stated
    scope.
-   **Denied** --- the role may not perform the action.
-   **Conditional** --- the backend must verify role, resource scope,
    ownership, workflow state, and prerequisites.

A hidden or disabled frontend control is not an access-control
mechanism. The backend must enforce every permission independently.

## 3. Role responsibilities

### `CUSTOMER`

Manages their own beneficiaries, requests indicative FX quotes, submits
transfer requests, views permitted details and history for their own
transfers, and responds to requests for missing information.

### `OPERATIONS`

Reviews eligible transfer requests, requests missing information,
records permitted operational decisions, refers flagged requests to
Compliance, and initiates or enables mock-provider processing only when
workflow prerequisites are met.

### `COMPLIANCE`

Inspects illustrative compliance holds, records a decision to maintain a
hold or clear an eligible hold, and accesses relevant compliance
history. Hold clearance requires a mandatory reason and a valid workflow
state.

### `ADMIN`

Performs approved application-level administration, such as managing
demo-user accounts and permitted role assignments. Admin privileges do
**not** automatically grant permission to clear compliance holds,
impersonate customers, approve transfers, or override workflow states.

## 4. Role-permission matrix

  -------------------------------------------------------------------------------------------------------
  Action                CUSTOMER    OPERATIONS    COMPLIANCE    ADMIN          Enforcement notes
  --------------------- ----------- ------------- ------------- -------------- --------------------------
  View own profile      Allowed     Allowed       Allowed       Allowed        Authenticated user may
                                                                               access their own profile.

  Manage own            Allowed     Denied        Denied        Denied by      Verify customer ownership
  beneficiaries                                                 default        server-side.

   Request indicative FX Allowed     Denied        Denied        Denied         Only the authenticated
   quote                                                                        customer requests quotes.

  Submit transfer       Allowed     Denied        Denied        Denied         Validate request and link
  request                                                                      it to the authenticated
                                                                               customer.

   View transfer details Own         Conditional   Denied        Conditional,   Compliance reads the
                         transfers                               read-only      compliance-specific hold
                         only                                                   projection, not the customer
                                                                                transfer endpoint.

   View transfer event   Own         Conditional   Allowed for    Conditional,   Compliance may read audit
   history               permitted                 transfers     authorised     events for transfers with a
                         events                    with a        audit scope    compliance hold, including
                                                   compliance                   reasons.
                                                   hold

  Respond to            Own         Denied        Denied        Denied         Permit only when the
  missing-information   eligible                                               workflow allows it.
  request               transfers                                              

   Request missing       Denied      Allowed       Denied        Denied         Only Operations requests
   information                                                                  missing information.

  Record routine        Denied      Allowed       Denied        Denied         Follow the state machine;
  operational review                                                           require reasons where
  decision                                                                     specified.

   Refer or flag for     Denied      Allowed       Hold decision  Denied         A hold decision is the
   compliance review                               only                         only compliance referral
                                                                                mechanism implemented.

   Place or maintain     Denied      Flag/refer    Allowed       Denied         Illustrative workflow
   compliance hold                   only                                       rule, not a legal
                                                                                determination.

  Clear compliance hold Denied      Denied        Allowed,      Denied         Requires COMPLIANCE role,
                                                  conditional                  valid held state, nonblank
                                                                               reason, and audit event.

  Initiate              Denied      Conditional   Denied        Denied by      Only eligible requests;
  mock-provider                                                 default        never dispatch real funds.
  processing                                                                   

   View mock-provider    Own         Conditional   Denied        Conditional,   Respect transfer-level
   outcome               permitted                               authorised     access restrictions.
                         transfer                                scope          
                         status                                                 

  Manage demo-user      Denied      Denied        Denied        Allowed,       Audit administrative
  accounts                                                      within         actions.
                                                                approved scope 

  Assign or change      Denied      Denied        Denied        Conditional    Restrict assignable roles,
  roles                                                                        prevent self-escalation,
                                                                               and audit changes.

  View security/audit   Denied      Conditional   Conditional   Conditional,   Do not expose secrets or
  information           except own                              authorised     unrelated customer data.
                        permitted                               scope          
                        history                                                

  Arbitrarily override  Denied      Denied        Denied        Denied         All state changes must
  transfer state                                                               follow defined
                                                                               transitions.

  Bypass ownership      Denied      Denied        Denied        Denied         No role bypasses backend
  checks                                                                       ownership/resource-scope
                                                                               enforcement.
  -------------------------------------------------------------------------------------------------------

## 5. Resolved scope decisions

The implemented prototype resolves the earlier proposals as follows:

1.  **ADMIN scope:** Administration is limited to the admin workspace.
    Admin cannot approve transfers, clear compliance holds, or override
    workflow states.
2.  **OPERATIONS visibility:** Operations can read the operations queue,
    but cannot clear compliance holds or dispatch a held transfer.
3.  **Conditional quote access:** Quote creation is customer-only.
4.  **COMPLIANCE visibility:** Compliance uses the dedicated
    `/api/v1/compliance` endpoints. It does not use customer or
    operations transfer endpoints.

## 6. Backend enforcement requirements

1.  **Authenticate protected requests.** Never trust a role, customer
    ID, or user ID supplied by the client as proof of identity or
    ownership.
2.  **Enforce role-based access.** Check the authenticated principal's
    granted permissions on the server.
3.  **Enforce resource-level access.** Customers can access only their
    own beneficiaries and transfers. Staff access is limited to the
    relevant operational or compliance scope.
4.  **Validate workflow state.** An authorised role cannot execute an
    action when the transfer is in an incompatible state.
5.  **Require reasons where specified.** Compliance hold clearance must
    reject blank or whitespace-only reasons.
6.  **Audit privileged actions.** Record actor, action, target resource,
    timestamp, outcome, and required reason. Normal application
    operations must not silently rewrite past events.
7.  **Protect role administration.** Restrict role assignments to
    approved values and prevent self-escalation or privilege escalation
    through mass assignment.
8.  **Use least privilege.** Grant only the permissions needed for each
    role's MVP workflow.
9.  **Fail closed.** Reject the operation if role, ownership, scope, or
    workflow validation fails.
10. **Keep the provider simulated.** The mock-provider interface must
    not dispatch, custody, settle, or disburse real funds.

## 7. Minimum authorisation test cases

  -----------------------------------------------------------------------
### Test-ID                 Scenario                Expected result
  ----------------------- ----------------------- -----------------------
   AUTH-01                 Customer A requests     Denied
                           Customer B's            
                           beneficiary             

   AUTH-02                 Customer A requests     Denied
                           Customer B's transfer   

   AUTH-03                 Customer submits a      Derive identity from
                           transfer with a forged  authenticated principal
                           customer ID             or reject mismatch

   AUTH-04                 Operations attempts to  Denied
                           clear a compliance hold 

   AUTH-05                 Compliance clears an    Allowed and audited
                           eligible hold with a    
                           valid reason            

   AUTH-06                 Compliance clears a     Denied
                           hold with a blank       
                           reason                  

   AUTH-07                 Admin attempts to clear Denied
                           a compliance hold       

   AUTH-08                 Admin attempts an       Denied
                           arbitrary               
                           transfer-state override 

   AUTH-09                 User attempts an        Denied; no invalid
                           invalid state           state change persisted
                           transition              

   AUTH-10                 Admin assigns a role    Denied and audited
                           outside the approved    where appropriate
                           set                     

   AUTH-11                 Admin attempts          Denied
                           unauthorised            
                           self-escalation         

   AUTH-12                 Duplicate submission or No duplicate workflow
                           provider event is       effect; handled
                           replayed                idempotently

   AUTH-13                 Unauthenticated caller  Denied
                           accesses a protected    
                           endpoint                

   AUTH-14                 Operations views a      Denied
                           transfer outside its    
                           permitted scope         

   AUTH-15                 User changes a          Protected field ignored
                            protected field through  or request rejected
                            mass assignment         

   AUTH-16                 User with COMPLIANCE     Allowed; decision actor
                            and another role        role is COMPLIANCE
                            clears a hold           

   AUTH-17                 Compliance reads a      Allowed with reasons;
                            held transfer's         other workspaces denied
                            audit history          

   -----------------------------------------------------------------------

## 8. Implementation notes

-   Implement role and ownership checks in Spring Security and the
    service layer; do not rely on the React UI.
-   Prefer explicit request DTOs and approved state-transition methods
    over arbitrary entity updates.
-   Keep compliance decisions separate from routine operations
    decisions.
-   Compliance endpoints are `GET /api/v1/compliance/queue`,
    `GET /dashboard`, `GET /transfers/{id}`,
    `GET /transfers/{id}/decisions`,
    `GET /transfers/{id}/audit-events`, `GET /audit-events`, and the
    `place-hold`, `maintain-hold`, and `clear-hold` POST actions.
-   Store status changes and privileged decisions in an auditable event
    history.
-   Use synthetic data only.
-   Treat illustrative compliance triggers as workflow demonstrations,
    not legal or regulatory determinations.

## 9. Approval checklist

-   [x] Four roles and their responsibilities are agreed.
-   [x] ADMIN scope is confirmed.
-   [x] OPERATIONS visibility model is confirmed.
-   [x] Conditional permissions have explicit business rules.
-   [x] Compliance clearance and mandatory-reason rules are agreed.
-   [x] Authorisation test cases are accepted for implementation.

------------------------------------------------------------------------

**Document boundary:** This matrix is a proposed MVP design. Review it
alongside the transfer state machine and API contract before
implementation. It is not legal, regulatory, or production security
certification.
