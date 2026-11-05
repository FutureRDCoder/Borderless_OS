1\. CUSTOMER
------------

Own resources only

1.  Sign in and view the customer dashboard.
    
2.  Add, view, update, or deactivate their own beneficiaries.
    
3.  Request an indicative FX quote and review the rate, fees, and quote expiry where available.
    
4.  Submit a transfer request.
    
5.  View their transfer status and permitted event history.
    
6.  Respond if Operations requests missing information.
    

2\. OPERATIONS
--------------

Operational workflow

1.  Sign in and open the review queue.
    
2.  Inspect an eligible submitted transfer.
    
3.  Request missing information with a reason, or record a permitted review decision.
    
4.  Route flagged requests to the Compliance workflow.
    
5.  View relevant transfer history and provider outcomes.
    
6.  Initiate or enable mock-provider processing only when workflow prerequisites are satisfied.
    

3\. COMPLIANCE
--------------

Restricted clearance authority

1.  Open the compliance queue.
    
2.  Inspect a held request and its illustrative trigger.
    
3.  Record a decision to maintain the hold or clear it, with a mandatory reason.
    
4.  Ensure clearance is permitted for the current state.
    
5.  View the decision history and associated actor and timestamp.
    

4\. ADMIN
---------

Application administration

1.  Manage permitted demo-user accounts and role assignments.
    
2.  Inspect application-level operational information.
    
3.  View security and audit information where authorised.
    
4.  Maintain administrative configuration within defined boundaries.
    

Restriction: Admin privileges do not automatically permit compliance clearance, customer impersonation, or arbitrary transfer-state changes.



# Important implementation rules

1.  A role name alone is insufficient: verify ownership and resource-level access on the server.
    
2.  Admin role assignment must be audited, restricted to approved roles, and protected against self-escalation.
    
3.  Compliance clearance requires the COMPLIANCE role, a valid held state, and a nonblank reason.
    
4.  Operations cannot clear compliance holds simply because a request is in their queue.
    
5.  No role can arbitrarily bypass valid state transitions.
    
6.  A UI button being hidden does not count as authorisation; the API must independently reject forbidden actions.