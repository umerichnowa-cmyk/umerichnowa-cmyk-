# Security Specification for AEIDC Pro

## Data Invariants
- A member document must have a unique `matricule`.
- `expirationDate` is always `31/12/2026`.
- Only the administrator (`umerichnowa@gmail.com`) can view the complete list of members.
- Validation on `fullName`, `email`, and `status`.

## The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: Attempting to list all members without logging in.
2. **Standard User Read**: Attempting to list all members as a standard user.
3. **Identity Spoofing**: Attempting to skip validation of the `matricule` field.
4. **Invalid Status**: Setting status to `GOD_MODE` instead of `EXPERT`.
5. **PII Leak**: Accessing another member's email address by guessing their ID.
6. **Self-Promotion**: An existing member trying to update their status from `ETUDIANT` to `EXPERT`.
7. **Bypassing Invariants**: Creating a member with an expiration date in 2030.
8. **Resource Exhaustion**: Sending a 1MB string in the `fullName` field.
9. **Document ID Poisoning**: Creating a member with an ID containing Malicious characters.
10. **Timestamp Faking**: Setting a manual `createdAt` far in the future.
11. **Admin Impersonation**: Attempting to perform update operations reserved for the admin.
12. **Blind Deletion**: A user trying to delete another user's membership data.

## Rules Logic
```javascript
function isAdmin() {
  return request.auth != null && request.auth.token.email == 'umerichnowa@gmail.com';
}

function isValidMember(data) {
  return data.fullName is string && data.fullName.size() <= 100 &&
         data.email is string && data.email.size() <= 100 &&
         data.status in ['ACTIF', 'STAGIAIRE', 'ETUDIANT', 'EXPERT'] &&
         data.expirationDate == '31/12/2026' &&
         data.matricule is string && data.matricule.size() <= 20;
}
```
