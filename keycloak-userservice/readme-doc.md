# TrackSwiftly Users Service API

## Invite User Endpoint

### Overview
Endpoint for inviting users to an organization within the TrackSwiftly system.

### Endpoint Details
- **Method**: POST
- **Path**: `/realms/<realmName>/users-services/invite-user`
- **Realm Requirements**: Realm name must contain both "Track" and "Swiftly" (case-insensitive)
  - Examples: `TrackSwiftlyRealm`, `SwiftlyTrackApp`, `TrackAppSwiftly`

### Authentication
- Requires a valid Bearer token
- Only users with ADMIN or MANAGER roles can access

### Request Parameters
- `email`: User's email address (required)
- `firstName`: User's first name (optional)
- `lastName`: User's last name (optional)

### Example CURL Request
```bash
curl --location '<base_url>/realms/<realmName>/users-services/invite-user' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header 'Authorization: Bearer <access_token>' \
--data-urlencode 'email=user@example.com'
```

### Notes
- Users can only be part of one organization
- Endpoint supports both inviting existing users and generating registration links
