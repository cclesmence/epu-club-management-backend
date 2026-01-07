# ✅ API Validate Token - Documentation

## 📋 Endpoint

**URL:** `GET /api/auth/validate`

**Description:** Validate JWT access token và trả về thông tin user nếu token hợp lệ.

---

## 🔐 Request

### Headers:
```
Authorization: Bearer {access_token}
```

### Example:
```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## ✅ Response Success (200 OK)

Token hợp lệ, user active.

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 7,
    "email": "tuannmhe173518@fpt.edu.vn",
    "fullName": "Nguyễn Minh Tuấn",
    "avatarUrl": "https://...",
    "systemRole": "STUDENT",
    "clubRoleList": [
      {
        "clubId": 1,
        "clubName": "F-Code (CLB Lập trình)",
        "clubRole": "Chủ nhiệm",
        "systemRole": "CLUB_OFFICER"
      }
    ]
  }
}
```

---

## ❌ Response Errors

### 1. Missing Authorization Header
```json
{
  "code": 401,
  "message": "Missing or invalid Authorization header",
  "data": null
}
```

### 2. Invalid Token Format
```json
{
  "code": 401,
  "message": "Invalid token format",
  "data": null
}
```

### 3. Token Expired
```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

### 4. Token Revoked (Blacklisted)
```json
{
  "code": 401,
  "message": "Token has been revoked",
  "data": null
}
```

### 5. User Not Found
```json
{
  "code": 401,
  "message": "User not found",
  "data": null
}
```

### 6. User Inactive
```json
{
  "code": 401,
  "message": "User account is inactive",
  "data": null
}
```

### 7. Server Error
```json
{
  "code": 500,
  "message": "Token validation error",
  "data": null
}
```

---

## 🔍 Validation Flow

```
1. Check Authorization Header
   ↓
2. Extract Token
   ↓
3. Extract Email from Token
   ↓
4. Check Token Blacklist (Revoked?)
   ↓
5. Find User in Database
   ↓
6. Check User Active Status
   ↓
7. Validate Token Signature & Expiration
   ↓
8. Load User's Club Roles
   ↓
9. Return User Info
```

---

## 🧪 Use Cases

### Use Case 1: Client-side Token Validation

**Scenario:** Frontend cần kiểm tra token còn hợp lệ không khi app khởi động.

```javascript
// Frontend code
async function validateToken() {
  const token = localStorage.getItem('accessToken');
  
  try {
    const response = await fetch('/api/auth/validate', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (response.ok) {
      const { data } = await response.json();
      // Token valid - Update user info
      setUser(data);
      return true;
    } else {
      // Token invalid - Redirect to login
      redirectToLogin();
      return false;
    }
  } catch (error) {
    console.error('Token validation failed:', error);
    return false;
  }
}
```

### Use Case 2: Protected Route Guard

```javascript
// React Router example
import { Navigate } from 'react-router-dom';

function ProtectedRoute({ children }) {
  const [isValid, setIsValid] = useState(null);
  
  useEffect(() => {
    validateToken().then(valid => setIsValid(valid));
  }, []);
  
  if (isValid === null) return <Loading />;
  if (!isValid) return <Navigate to="/login" />;
  
  return children;
}
```

### Use Case 3: Periodic Token Check

```javascript
// Check token every 5 minutes
setInterval(async () => {
  const valid = await validateToken();
  if (!valid) {
    // Try refresh token first
    const refreshed = await refreshToken();
    if (!refreshed) {
      // Redirect to login
      window.location.href = '/login';
    }
  }
}, 5 * 60 * 1000);
```

### Use Case 4: API Gateway Integration

```javascript
// API Gateway can call this endpoint to validate tokens
// before forwarding requests to other microservices

async function apiGatewayMiddleware(req, res, next) {
  const token = req.headers.authorization;
  
  try {
    const response = await fetch('http://auth-service/api/auth/validate', {
      headers: { 'Authorization': token }
    });
    
    if (response.ok) {
      const { data } = await response.json();
      req.user = data; // Attach user info to request
      next();
    } else {
      res.status(401).json({ error: 'Unauthorized' });
    }
  } catch (error) {
    res.status(500).json({ error: 'Auth service unavailable' });
  }
}
```

---

## 🔐 Security Features

### 1. Token Blacklist Check ✅
- Kiểm tra token đã bị logout/revoke chưa
- Dùng Redis để lưu blacklist với TTL = token expiration

### 2. User Active Check ✅
- Kiểm tra user có bị disable không
- Ngăn user bị deactivate tiếp tục dùng token cũ

### 3. Signature Validation ✅
- Kiểm tra token signature với secret key
- Đảm bảo token không bị giả mạo

### 4. Expiration Check ✅
- Kiểm tra token có hết hạn chưa
- Reject token đã expired

### 5. Role Loading ✅
- Load fresh club roles từ database
- Đảm bảo permissions up-to-date

---

## 📊 Performance

### Expected Response Time:
- Valid token: ~50-100ms
- Invalid token: ~10-20ms (fail fast)

### Caching Strategy:
- **NOT** recommended to cache validation results
- Always validate against latest data (blacklist, user status, roles)

### Rate Limiting:
- Recommended: 100 requests/minute per user
- Prevent brute force attacks

---

## 🧪 Testing

### Test Case 1: Valid Token ✅
```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer {valid_token}"

Expected: 200 OK with user info
```

### Test Case 2: Expired Token ❌
```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer {expired_token}"

Expected: 401 Unauthorized - "Invalid or expired token"
```

### Test Case 3: Revoked Token ❌
```bash
# User logout first
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer {token}"

# Then validate same token
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer {token}"

Expected: 401 Unauthorized - "Token has been revoked"
```

### Test Case 4: No Token ❌
```bash
curl -X GET http://localhost:8080/api/auth/validate

Expected: 401 Unauthorized - "Missing or invalid Authorization header"
```

### Test Case 5: Invalid Format ❌
```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: InvalidFormat"

Expected: 401 Unauthorized - "Missing or invalid Authorization header"
```

### Test Case 6: User Deactivated ❌
```bash
# Admin deactivates user first
# Then user validates token

curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer {token}"

Expected: 401 Unauthorized - "User account is inactive"
```

---

## 📝 Notes

### When to use this API:

✅ **DO use:**
- App startup/initialization
- Route guard checks
- Periodic token validation
- Before critical operations
- After long idle time

❌ **DON'T use:**
- Every single API call (too expensive)
- Already have token in SecurityContext
- Inside backend services (use filter instead)

### Difference from JWT Filter:

| Feature | `/auth/validate` API | JwtAuthenticationFilter |
|---------|---------------------|-------------------------|
| Purpose | Explicit validation endpoint | Automatic filter for all requests |
| When | On-demand by client | Every protected request |
| Response | UserInfo JSON | Sets SecurityContext |
| Use case | Frontend token check | Backend authentication |

### Best Practices:

1. **Frontend should validate token:**
   - On app load
   - After refresh token
   - Before showing protected content

2. **Backend uses filter automatically:**
   - No need to call validate in every controller
   - SecurityContext already has user info

3. **Combine with refresh token:**
   - If validate fails, try refresh
   - Only logout if refresh also fails

---

## 🔄 Integration with Existing Auth Flow

```
Login → Access Token + Refresh Token
   ↓
Use Access Token for requests
   ↓
(Optional) Validate token periodically ← NEW API
   ↓
Token expires?
   ↓
Try Refresh Token → New Access Token
   ↓
Refresh fails?
   ↓
Redirect to Login
```

---

**Created:** November 23, 2025  
**Endpoint:** `GET /api/auth/validate`  
**Status:** ✅ READY TO USE  
**Security Level:** HIGH  
**Rate Limit:** Recommended 100/min

