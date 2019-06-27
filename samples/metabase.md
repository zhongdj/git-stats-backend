# Metabase RESTful API Examples

```
https://www.getpostman.com/collections/f9d669472cfab7d84baf
# Login
```
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"username": "cam@metabase.com", "password": "myPassword"}' \
  http://localhost:3000/api/session 
```

